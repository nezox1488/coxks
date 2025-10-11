package fun.rich.utils.display.scissor;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import fun.rich.utils.display.interfaces.QuickImports;

import java.util.Stack;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ScissorAssist implements QuickImports {
    Pool<Scissor> scissorPool = new Pool<>(Scissor::new);
    Stack<Scissor> scissorStack = new Stack<>();

    public void push(Matrix4f matrix4f, float x, float y, float width, float height) {
        Scissor newScissor = scissorPool.get().copy();
        Vector3f pos = matrix4f.transformPosition(x, y, 0, new Vector3f());
        Vector3f size = matrix4f.getScale(new Vector3f()).mul(width, height, 0);
        newScissor.set(pos.x, pos.y, size.x, size.y);

        if (!scissorStack.isEmpty()) {
            Scissor currentScissor = scissorStack.peek().copy();

            int newX = Math.max(currentScissor.x, newScissor.x);
            int newY = Math.max(currentScissor.y, newScissor.y);
            int newWidth = Math.min(currentScissor.x + currentScissor.width, newScissor.x + newScissor.width) - newX;
            int newHeight = Math.min(currentScissor.y + currentScissor.height, newScissor.y + newScissor.height) - newY;

            if (newWidth <= 0 || newHeight <= 0) {
                newScissor.set(0, 0, 0, 0);
            } else {
                newScissor.set(newX, newY, newWidth, newHeight);
            }
        }

        scissorStack.push(newScissor);
        setScissor(newScissor);
    }

    public void pop() {
        if (!scissorStack.isEmpty()) {
            scissorPool.free(scissorStack.pop());
            if (scissorStack.isEmpty()) {
                RenderSystem.disableScissor();
            } else {
                setScissor(scissorStack.peek());
            }
        }
    }

    private void setScissor(Scissor scissor) {
        int scaleFactor = (int) window.getScaleFactor();
        int x = scissor.x * scaleFactor;
        int y = window.getHeight() - (scissor.y * scaleFactor + scissor.height * scaleFactor);
        int width = scissor.width * scaleFactor;
        int height = scissor.height * scaleFactor;

        if (width > 0 && height > 0) {
            RenderSystem.enableScissor(x, y, width, height);
        } else {
            RenderSystem.disableScissor();
        }
    }

    private static class Scissor {
        public int x, y;
        public int width, height;

        public void set(double x, double y, double width, double height) {
            this.x = Math.max(0, (int) Math.round(x));
            this.y = Math.max(0, (int) Math.round(y));
            this.width = Math.max(0, (int) Math.round(width));
            this.height = Math.max(0, (int) Math.round(height));
        }

        Scissor copy() {
            Scissor newScissor = new Scissor();
            newScissor.set(this.x, this.y, this.width, this.height);
            return newScissor;
        }
    }
}