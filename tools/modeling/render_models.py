import bpy, json, math, pathlib
from mathutils import Vector
root=pathlib.Path(__file__).resolve().parents[2]
data=json.loads((root/'models/model_geometry.json').read_text())
bpy.ops.object.select_all(action='SELECT'); bpy.ops.object.delete(use_global=False)
materials={}
for name,color in data['palette'].items():
    def linear(v): return v/12.92 if v<=.04045 else ((v+.055)/1.055)**2.4
    material=bpy.data.materials.new(name); material.diffuse_color=tuple(linear(int(color[i:i+2],16)/255) for i in (1,3,5))+(1,)
    material.use_nodes=True
    material.node_tree.nodes.get('Principled BSDF').inputs['Base Color'].default_value=material.diffuse_color
    material.node_tree.nodes.get('Principled BSDF').inputs['Roughness'].default_value=.8
    if name.startswith('heated_'):
        material.node_tree.nodes.get('Principled BSDF').inputs['Emission Color'].default_value=material.diffuse_color
        material.node_tree.nodes.get('Principled BSDF').inputs['Emission Strength'].default_value=.35
    materials[name]=material
def assemble(kind,z,heat='metal',rotate=False):
    collection=bpy.data.collections.new(kind+'_'+heat); bpy.context.scene.collection.children.link(collection)
    for index,part in enumerate(data[kind]):
        a,b=part['from'],part['to']; center=[(a[i]+b[i])/2-8 for i in range(3)]
        position=Vector((center[0]/8,center[2]/8,center[1]/8))
        dimensions=Vector(((b[0]-a[0])/8,(b[2]-a[2])/8,(b[1]-a[1])/8))
        if rotate: position=Vector((-position.y,position.x,position.z))
        bpy.ops.mesh.primitive_cube_add(size=1,location=position+Vector((0,0,z)))
        obj=bpy.context.object; obj.name=part['name']+'_'+str(index); obj.dimensions=dimensions
        if rotate: obj.rotation_euler.z=math.pi/2
        key=next(iter(part['faces'].values()))['texture'][1:]
        obj.data.materials.append(materials[key.removeprefix('heated_') if heat=='metal' else key])
        for existing in list(obj.users_collection): existing.objects.unlink(obj)
        collection.objects.link(obj)
assemble('launcher',2.3)
assemble('launcher',.9,'red')
assemble('rocket',-.2,rotate=True)
bpy.ops.object.camera_add(location=(-4,-9,5.4))
camera=bpy.context.object; camera.rotation_euler=(Vector((0,0,1.1))-camera.location).to_track_quat('-Z','Y').to_euler()
camera.data.type='ORTHO';camera.data.ortho_scale=5.6; bpy.context.scene.camera=camera
for position,power,size in [((-3,-4,8),1100,5),((4,2,6),750,4)]:
    bpy.ops.object.light_add(type='AREA',location=position); lamp=bpy.context.object;lamp.data.energy=power;lamp.data.shape='DISK';lamp.data.size=size
    lamp.rotation_euler=(-lamp.location).to_track_quat('-Z','Y').to_euler()
scene=bpy.context.scene;scene.render.engine='CYCLES';scene.cycles.samples=24
scene.world.color=(.5,.5,.5);scene.render.film_transparent=True
scene.render.resolution_x=1000;scene.render.resolution_y=1000;scene.render.resolution_percentage=100
scene.render.image_settings.file_format='PNG';scene.render.filepath=str(root/'models/launcher_preview.png')
bpy.ops.wm.save_as_mainfile(filepath=str(root/'models/weapon_design.blend'))
bpy.ops.render.render(write_still=True)
