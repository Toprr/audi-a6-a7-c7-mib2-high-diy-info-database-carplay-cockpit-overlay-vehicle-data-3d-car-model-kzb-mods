# GPU renderer (moderngl): smooth normals, MSAA, depth buffer, paint with environment reflection
import numpy as np, moderngl, cv2
_ctx=None
def ctx():
    global _ctx
    if _ctx is None: _ctx=moderngl.create_standalone_context()
    return _ctx
VS='''
#version 330
uniform mat4 mvp; in vec3 pos; in vec3 nrm; in vec3 col; in float shine;
out vec3 vN; out vec3 vP; out vec3 vC; out float vS;
void main(){ gl_Position=mvp*vec4(pos,1.0); vN=nrm; vP=pos; vC=col; vS=shine; }'''
FS='''
#version 330
uniform vec3 eye; in vec3 vN; in vec3 vP; in vec3 vC; in float vS; out vec4 o;
vec3 sky(vec3 d){ float t=clamp(d.y*0.5+0.5,0.0,1.0); vec3 g=vec3(0.16,0.16,0.17); vec3 h=vec3(0.75,0.78,0.82); vec3 z=vec3(0.45,0.55,0.70);
  return d.y<0.0? mix(h*0.55,g,clamp(-d.y*3.0,0.0,1.0)) : mix(h,z,pow(t,0.6)); }
void main(){
  vec3 N=normalize(vN); vec3 V=normalize(eye-vP); if(dot(N,V)<0.0) N=-N;
  vec3 L1=normalize(vec3(-0.4,0.9,0.5)); vec3 L2=normalize(vec3(0.6,0.4,-0.7));
  float d=max(dot(N,L1),0.0)*0.75+max(dot(N,L2),0.0)*0.30+0.22;
  vec3 R=reflect(-V,N); float F=0.04+0.96*pow(1.0-max(dot(N,V),0.0),5.0);
  vec3 env=sky(R);
  float sp=pow(max(dot(N,normalize(L1+V)),0.0),80.0)+0.5*pow(max(dot(N,normalize(L2+V)),0.0),60.0);
  vec3 c=vC*d + vS*(env*(0.10+0.55*F)+vec3(sp)*0.8);
  o=vec4(pow(c,vec3(1.0/1.15)),vS<-0.5?0.0:1.0);
}'''
def corner_normals(V,F,thr=0.55):
    T=V[F]; fn=np.cross(T[:,1]-T[:,0],T[:,2]-T[:,0])
    key=np.round(V/2e-4).astype(np.int64); _,wid=np.unique(key,axis=0,return_inverse=True); wid=wid.ravel()
    Fw=wid[F]; vn=np.zeros((wid.max()+1,3))
    for k in range(3): np.add.at(vn,Fw[:,k],fn)
    vn/=np.linalg.norm(vn,axis=1,keepdims=True)+1e-12; F=Fw
    fnu=fn/(np.linalg.norm(fn,axis=1,keepdims=True)+1e-12)
    N=vn[F]                                   # (n,3,3)
    dot=(N*fnu[:,None,:]).sum(-1)
    # avoid flips: align vertex normals with the face normal
    N=np.where((dot<0)[...,None],-N,N); dot=np.abs(dot)
    N=np.where((dot<thr)[...,None],fnu[:,None,:],N)
    return N.astype(np.float32)
def look(eye,tgt,up=(0,1,0)):
    f=tgt-eye; f/=np.linalg.norm(f); s=np.cross(f,up); s/=np.linalg.norm(s); u=np.cross(s,f)
    M=np.eye(4); M[0,:3]=s; M[1,:3]=u; M[2,:3]=-f; M[:3,3]=-M[:3,:3]@eye; return M
def persp(fov,a,n,f):
    t=1/np.tan(np.radians(fov)/2); M=np.zeros((4,4)); M[0,0]=t/a; M[1,1]=t; M[2,2]=(f+n)/(n-f); M[2,3]=2*f*n/(n-f); M[3,2]=-1; return M
def pack(sc):
    P=[];N=[];C=[];S=[]
    for V,F,start in zip(sc.V,sc.F,np.cumsum([0]+[len(v) for v in sc.V])[:-1]):
        Fl=F-start; P.append(V[Fl].reshape(-1,3)); N.append(corner_normals(V,Fl).reshape(-1,3))
    C=np.repeat(np.array(sc.C,np.float32)[:,::-1]/255.0,3,axis=0)   # BGR->RGB
    S=np.repeat(np.array(sc.S,np.float32),3)
    return np.vstack(P).astype(np.float32),np.vstack(N).astype(np.float32),C,S
def render(sc,yaw,pitch,W=1280,H=720,zoom=1.0,center=None,fov=28,ground=True,alpha=False):
    c=ctx(); P,N,C,S=pack(sc)
    mid=(P.max(0)+P.min(0))/2; tgt=mid if center is None else np.array(center,np.float32)
    rad=np.linalg.norm(P.max(0)-P.min(0))/2
    a,b=np.radians(yaw),np.radians(pitch)
    # yaw: looking towards -z at yaw=0 -> camera on the +z side
    dirv=np.array([-np.sin(a)*np.cos(b), np.sin(b), np.cos(a)*np.cos(b)])
    dist=rad/np.tan(np.radians(fov)/2)*0.62/zoom
    eye=tgt+dirv*dist
    mvp=(persp(fov,W/H,0.05,100)@look(eye,tgt)).T.astype(np.float32)
    prog=c.program(vertex_shader=VS,fragment_shader=FS)
    prog['mvp'].write(mvp.tobytes()); prog['eye'].value=tuple(eye.astype(float))
    data=np.hstack([P,N,C,S[:,None]]).astype(np.float32)
    if ground:
        g=0.0; R=12
        gp=np.array([[-R,g,-R],[R,g,-R],[R,g,R],[-R,g,-R],[R,g,R],[-R,g,R]],np.float32)
        gd=np.hstack([gp,np.tile([0,1,0],(6,1)),np.tile([0.10,0.10,0.11],(6,1)),np.zeros((6,1))]).astype(np.float32)
        data=np.vstack([data,gd])
    vbo=c.buffer(data.tobytes())
    vao=c.vertex_array(prog,[(vbo,'3f 3f 3f 1f','pos','nrm','col','shine')])
    ms=c.framebuffer(color_attachments=[c.renderbuffer((W,H),samples=8)],depth_attachment=c.depth_renderbuffer((W,H),samples=8))
    out=c.framebuffer(color_attachments=[c.texture((W,H),4)])
    ms.use(); c.enable(moderngl.DEPTH_TEST); c.clear(0.12,0.12,0.13,0.0); vao.render()
    c.copy_framebuffer(out,ms)
    img=np.frombuffer(out.read(components=4),np.uint8).reshape(H,W,4)[::-1]
    for o in (vao,vbo,ms,out,prog): o.release()
    if alpha: return cv2.cvtColor(img,cv2.COLOR_RGBA2BGRA)
    return cv2.cvtColor(img[:,:,:3].copy(),cv2.COLOR_RGB2BGR)


