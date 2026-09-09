import bpy,json,pathlib
from mathutils import Vector
root=pathlib.Path(__file__).resolve().parents[2]
# Extend the existing Blender project, retaining its rocket launcher and rocket round.
bpy.ops.wm.open_mainfile(filepath=str(root/'models/weapon_design.blend'))
data=json.loads((root/'target/modeling/annihilation_geometry.json').read_text(encoding='utf-8'))
for obj in bpy.context.scene.objects:obj.hide_render=True
collection=bpy.data.collections.new('寂灭');bpy.context.scene.collection.children.link(collection)
materials={}
for name,color in data['palette'].items():
    material=bpy.data.materials.new('寂灭_'+name);material.use_nodes=True
    rgb=tuple((int(color[i:i+2],16)/255)**2.2 for i in (1,3,5))+(1,)
    shader=material.node_tree.nodes.get('Principled BSDF');shader.inputs['Base Color'].default_value=rgb;shader.inputs['Roughness'].default_value=.55
    if name.startswith('flow'):
        shader.inputs['Emission Color'].default_value=(1,.008,.025,1);shader.inputs['Emission Strength'].default_value=2
    materials[name]=material
for part in data['launcher']:
    a,b=part['from'],part['to'];center=[(a[i]+b[i])/2-8 for i in range(3)]
    bpy.ops.mesh.primitive_cube_add(size=1,location=(center[0]/8,center[2]/8,center[1]/8))
    obj=bpy.context.object;obj.name=part['name'];obj.dimensions=((b[0]-a[0])/8,(b[2]-a[2])/8,(b[1]-a[1])/8)
    obj.data.materials.append(materials[next(iter(part['faces'].values()))['texture'][1:]])
    for previous in list(obj.users_collection):previous.objects.unlink(obj)
    collection.objects.link(obj)
bpy.ops.object.camera_add(location=(-3.6,-8,3.0));camera=bpy.context.object
camera.rotation_euler=(Vector((.2,0,0))-camera.location).to_track_quat('-Z','Y').to_euler();camera.data.type='ORTHO';camera.data.ortho_scale=4.6
bpy.context.scene.camera=camera
for pos,energy in [((-3,-4,6),1800),((4,2,5),2200)]:
    bpy.ops.object.light_add(type='AREA',location=pos);obj=bpy.context.object;obj.data.energy=energy;obj.data.shape='DISK';obj.data.size=5
    obj.rotation_euler=(-obj.location).to_track_quat('-Z','Y').to_euler()
scene=bpy.context.scene;scene.render.engine='CYCLES';scene.cycles.samples=16
scene.render.resolution_x=1200;scene.render.resolution_y=650;scene.render.resolution_percentage=100
scene.render.film_transparent=False;scene.world.color=(.1,.1,.1)
scene.render.filepath=str(root/'models/annihilation_preview.png')
bpy.ops.wm.save_as_mainfile(filepath=str(root/'models/寂灭.blend'))
bpy.ops.render.render(write_still=True)
