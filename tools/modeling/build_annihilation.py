"""Editable Blockbench / Minecraft geometry; matched to the supplied pixel silhouette."""
import copy, json, math
import build_models as base

root=base.ROOT
colors={'abyss':'#070a12','armor':'#171d2a','edge':'#424a61','inset':'#0d121d','trim':'#778298',
        'cold':'#849b93','redline':'#ff263c','white':'#fff4eb','black':'#000000'}
base.PALETTE.update(colors)
for key,color in colors.items():
    path=base.ASSETS/f'textures/item/palette/{key}.png';path.write_bytes(base.png(color))
gun=[]
def g(n,a,b,t): gun.append(base.cube(n,a,b,t))
# Front is negative X, consistent with the existing rocket launcher poses.
g('monolithic upper rail',[-5,10.6,5],[20,11.6,11],'abyss')
g('upper chamfer highlight',[-4.8,11.6,5.5],[19.5,12,6.3],'edge')
g('lower rail',[-4,4.8,5],[20,5.8,11],'abyss')
g('lower chamfer',[-3.5,4.5,5.6],[19.3,4.8,6.3],'edge')
g('inner core',[-2,6.1,6.2],[19,10.3,9.8],'inset')
g('recessed side panel',[-1.5,6.4,5.5],[18.8,10,6.2],'armor')
g('far side panel',[-1.5,6.4,9.8],[18.8,10,10.5],'armor')
for z in [4.4,10.6]:
    g('muzzle collar upright',[-5,5.5,z],[-3.9,10.8,z+1],'edge')
g('muzzle collar top',[-5,10.6,4.4],[-3.9,11.6,11.6],'trim')
g('muzzle collar bottom',[-5,4.8,4.4],[-3.9,5.8,11.6],'edge')
g('muzzle dark throat',[-3.8,6,6],[-3.4,10.4,10],'abyss')
g('muzzle core slit',[-3.81,7.65,6.7],[-3.79,8.65,9.3],'cold')
g('carry handle front',[3,12,7],[4.1,13.9,9],'abyss')
g('carry handle rear',[9.3,12,7],[10.4,13.9,9],'abyss')
g('carry handle',[3,13.7,7],[10.4,14.5,9],'abyss')
g('carry handle glint',[3.2,13.5,6.8],[10.2,13.9,7],'edge')
g('grip receiver',[10.8,3.8,6.1],[16.5,4.9,9.9],'armor')
g('pistol grip',[13.4,.3,7],[15.6,3.8,9],'abyss')
g('grip bevel',[13.2,.8,6.8],[13.8,3.4,7],'edge')
g('trigger guard',[10.2,1.8,7],[10.7,4.5,9],'abyss')
g('trigger guard base',[10.3,1.5,7],[13.8,2,9],'abyss')
g('shoulder stop',[19,3.8,5.8],[20.4,6.5,10.2],'edge')
for x in [3,9,16]:
    for z in [5.2,10.5]:
        g('energy window bezel',[x,7,z],[x+2.3,9.5,z+.3],'abyss')
        g('energy window glass',[x+.35,7.6,z-.02],[x+1.8,8.7,z+.32],'cold')
for band in range(12):
    x=-3.5+band*1.85
    for z in [5.05,10.8]:
        y=8.0 if band<4 else 8.25 if band<8 else 8.5
        g(f'flow {band:02d}',[x,y,z],[x+1.7,y+.24,z+.15],f'flow{band}')
    g(f'dorsal flow {band:02d}',[x,12.01,7.75],[x+1.7,12.12,8.25],f'flow{band}')

display=copy.deepcopy(base.display)
display['gui']={'rotation':[0,180,0],'translation':[0,-1,0],'scale':[.6,.6,.6]}
def model(charge=0,cool=0):
    textures={k:'uglobalweapon:item/palette/'+k for k in base.PALETTE}
    for band in range(12):
        key=f'flow_{charge}_{cool}_{band}'
        amount=(max(0,min(1,charge-band)) if charge else 0)*(1-cool/12)
        rgb=[round(a*(1-amount)+b*amount) for a,b in zip((100,125,125),(255,22,48))]
        color='#'+''.join(f'{c:02x}' for c in rgb)
        path=base.ASSETS/f'textures/item/energy/{key}.png';path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(base.png(color))
        textures[f'flow{band}']=f'uglobalweapon:item/energy/{key}'
    textures['particle']='uglobalweapon:item/palette/abyss'
    return {'gui_light':'side','textures':textures,'display':display,'elements':gun}
base.save(base.MODELS/'annihilation_idle.json',model())
overrides=[]
def override(n,name): overrides.append({'predicate':{'custom_model_data':n},'model':'uglobalweapon:item/'+name})
override(3001,'annihilation_idle')
for i in range(5):
    pose=copy.deepcopy(base.baseframes[1002+i]);pose['parent']='uglobalweapon:item/annihilation_idle'
    base.save(base.MODELS/f'annihilation_raise_{i}.json',pose);override(3002+i,f'annihilation_raise_{i}')
base.save(base.MODELS/'annihilation_hot.json',model(12))
for i in range(4):
    pose=copy.deepcopy(base.baseframes[1020+i]);pose['parent']='uglobalweapon:item/annihilation_hot'
    base.save(base.MODELS/f'annihilation_recoil_{i}.json',pose);override(3020+i,f'annihilation_recoil_{i}')
for i in range(12):
    base.save(base.MODELS/f'annihilation_charge_{i}.json',model(i+1));override(3040+i,f'annihilation_charge_{i}')
    base.save(base.MODELS/f'annihilation_cool_{i}.json',model(12,i+1));override(3060+i,f'annihilation_cool_{i}')
# A solid voxel sphere, not a particle cloud: native block geometry keeps the pixel style.
sphere=[]
steps=24
for y in range(steps):
    for x in range(steps):
        px=(x+.5)*2/steps-1;py=(y+.5)*2/steps-1
        q=1-px*px-py*py
        if q<=0: continue
        depth=math.sqrt(q)*8
        sphere.append(base.cube('event horizon',[x*16/steps,y*16/steps,8-depth],[(x+1)*16/steps,(y+1)*16/steps,8+depth],'black'))
base.save(base.MODELS/'annihilation_sphere.json',{'textures':{'black':'uglobalweapon:item/palette/black'},'elements':sphere})
override(4001,'annihilation_sphere')
beam=[base.cube('beam core',[4,4,8],[12,12,24],'white')]
for a,b in [([0,0,8],[2,16,24]),([14,0,8],[16,16,24]),([2,0,8],[14,2,24]),([2,14,8],[14,16,24])]:beam.append(base.cube('beam corona',a,b,'redline'))
base.save(base.MODELS/'annihilation_beam.json',{'textures':{k:'uglobalweapon:item/palette/'+k for k in ['white','redline']},'elements':beam})
override(4002,'annihilation_beam')
path=root/'resource-pack/assets/minecraft/models/item/nether_star.json'
star=json.loads(path.read_text());star['overrides']+=sorted(overrides,key=lambda o:o['predicate']['custom_model_data']);base.save(path,star)
for band in range(12):base.PALETTE[f'flow{band}']=colors['cold']
base.blockbench('寂灭',gun,display)
base.save(root/'target/modeling/annihilation_geometry.json',{'palette':base.PALETTE,'launcher':gun})
print('Built 寂灭: 12 charge stages, cooling, raised/recoil poses, solid singularity and beam.')
