import numpy as np
def slice_mesh(V,F,axis,val):
    """Schnittpunkte aller Dreiecke mit Ebene coord[axis]=val"""
    T=V[F]; d=T[:,:,axis]-val; pts=[]
    for a,b in ((0,1),(1,2),(2,0)):
        da,db=d[:,a],d[:,b]; m=(da*db)<0
        t=da[m]/(da[m]-db[m]); p=T[m,a]+(T[m,b]-T[m,a])*t[:,None]; pts.append(p)
    return np.vstack(pts)
