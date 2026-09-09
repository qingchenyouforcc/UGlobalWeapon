import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import static org.lwjgl.opengl.GL33C.*;
import java.nio.*;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/** One-shot shader compilation and synthetic-scene preview in an invisible GL window. */
public class LensShaderPreview {
    static int compile(int type,String source) {
        int shader=glCreateShader(type);glShaderSource(shader,source);glCompileShader(shader);
        if(glGetShaderi(shader,GL_COMPILE_STATUS)==0) throw new IllegalStateException(glGetShaderInfoLog(shader));
        return shader;
    }
    static void value(int p,String name,float value) {glUniform1f(glGetUniformLocation(p,name),value);}
    public static void main(String[] args) throws Exception {
        if(!GLFW.glfwInit()) throw new IllegalStateException("No GLFW context");
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE,GLFW.GLFW_FALSE);
        long window=GLFW.glfwCreateWindow(1000,640,"Lens shader check",0,0);
        if(window==0) throw new IllegalStateException("No hidden GL window");
        try {
            GLFW.glfwMakeContextCurrent(window);GL.createCapabilities();
            int vertex=compile(GL_VERTEX_SHADER,"#version 150\nin vec2 Position;out vec2 texCoord;void main(){texCoord=Position*0.5+0.5;gl_Position=vec4(Position,0,1);}");
            int fragment=compile(GL_FRAGMENT_SHADER,Files.readString(Path.of(args[0])));
            int program=glCreateProgram();glAttachShader(program,vertex);glAttachShader(program,fragment);glBindAttribLocation(program,0,"Position");glLinkProgram(program);
            if(glGetProgrami(program,GL_LINK_STATUS)==0) throw new IllegalStateException(glGetProgramInfoLog(program));
            glUseProgram(program);
            int w=1000,h=640;float aspect=(float)w/h;
            ByteBuffer color=BufferUtils.createByteBuffer(w*h*4);FloatBuffer depth=BufferUtils.createFloatBuffer(w*h);
            for(int y=0;y<h;y++) for(int x=0;x<w;x++) {
                double u=(x+.5)/w,v=(y+.5)/h,r=Math.hypot((u-.5)*aspect,v-.5);
                int checker=((x/40+y/40)&1);int red=30+checker*35,green=50+checker*40,blue=v>.48?105+checker*50:45+checker*25;
                if(x%40<2 || y%40<2) {red=90;green=120;blue=160;}
                if(r<.13) red=green=blue=0;
                boolean foreground=x>700 && x<735 && y<450;
                if(foreground) {red=105;green=70;blue=45;}
                color.put((byte)red).put((byte)green).put((byte)blue).put((byte)255);depth.put(foreground?.5F:r<.13?.92F:1F);
            }
            color.flip();depth.flip();
            int tex=glGenTextures();glActiveTexture(GL_TEXTURE0);glBindTexture(GL_TEXTURE_2D,tex);
            glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA8,w,h,0,GL_RGBA,GL_UNSIGNED_BYTE,color);
            glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR);
            glUniform1i(glGetUniformLocation(program,"DiffuseSampler"),0);
            int dep=glGenTextures();glActiveTexture(GL_TEXTURE1);glBindTexture(GL_TEXTURE_2D,dep);
            glTexImage2D(GL_TEXTURE_2D,0,GL_R32F,w,h,0,GL_RED,GL_FLOAT,depth);
            glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_NEAREST);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_NEAREST);
            glUniform1i(glGetUniformLocation(program,"DepthSampler"),1);
            value(program,"LensX",.5F);value(program,"LensY",.5F);value(program,"LensRadius",.13F);value(program,"LensDepth",.9F);
            value(program,"Aspect",aspect);value(program,"DiskAxisX",1);value(program,"DiskAxisY",0);
            value(program,"DiskInclination",.24F);value(program,"DiskSide",1);value(program,"OrbitTime",3);
            glUniform2f(glGetUniformLocation(program,"OutSize"),w,h);
            int vao=glGenVertexArrays();glBindVertexArray(vao);int vbo=glGenBuffers();glBindBuffer(GL_ARRAY_BUFFER,vbo);
            glBufferData(GL_ARRAY_BUFFER,new float[]{-1,-1,3,-1,-1,3},GL_STATIC_DRAW);glEnableVertexAttribArray(0);glVertexAttribPointer(0,2,GL_FLOAT,false,0,0);
            glViewport(0,0,w,h);glDrawArrays(GL_TRIANGLES,0,3);glFinish();
            ByteBuffer result=BufferUtils.createByteBuffer(w*h*4);glReadPixels(0,0,w,h,GL_RGBA,GL_UNSIGNED_BYTE,result);
            BufferedImage image=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
            for(int y=0;y<h;y++) for(int x=0;x<w;x++) {int i=(y*w+x)*4;image.setRGB(x,h-1-y,((result.get(i)&255)<<16)|((result.get(i+1)&255)<<8)|(result.get(i+2)&255));}
            ImageIO.write(image,"PNG",Path.of(args[1]).toFile());
            if(glGetError()!=GL_NO_ERROR) throw new IllegalStateException("OpenGL error");
            System.out.println("Shader compiled, linked and rendered: "+glGetString(GL_RENDERER));
        } finally {GLFW.glfwDestroyWindow(window);GLFW.glfwTerminate();}
    }
}
