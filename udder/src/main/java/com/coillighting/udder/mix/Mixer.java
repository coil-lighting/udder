package com.coillighting.udder.mix;

import java.util.Collection;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

import com.coillighting.udder.blend.MaxBlendOp;
import com.coillighting.udder.infrastructure.Stateful;
import com.coillighting.udder.model.Device;
import com.coillighting.udder.model.Pixel;

/** A concrete scenegraph is implemented as a Mixer with one or more Layers.
 *  Each layer holds an Animator, ordinarily an effect plug-in which draws
 *  part of the scene. The Mixer then composites the complete scene by blending
 *  each layer in turn with the output of earlier blend operations.
 */
public class Mixer extends MixableBase implements Mixable, Iterable<Mixable> {

    /** In order of composition, i.e. first element is the background layer,
     *  last element is the foreground layer.
     */
    protected ArrayList<Mixable> layers;
    protected Pixel[] pixels; // the developing frame
    protected int deviceCount = 0;

    /** Additional objects may sign up to be animated in synch with the layers.
     * They are always animated before the layers, in the order in which they
     * subscribed. It is permissible for a subscriber to manipulate this Mixer
     * as long as it keeps its hands off the list of subscribers. For example,
     * a shuffle mode subscriber might randomly turn up some layers and
     * turn down others, or an LFO subscriber might periodically fade up and
     * down the mixer's master level.
     */
    protected List<StatefulAnimator> subscribers;

    public Mixer(Collection<Mixable> layers) {

        this.layers = new ArrayList<Mixable>(layers);
        this.setBlendOp(new MaxBlendOp());
        this.subscribers = new ArrayList<StatefulAnimator>();
    }

    public void subscribeAnimator(StatefulAnimator a) {
        subscribers.add(a);
    }

    public Class getStateClass() {
        return LayerState.class; // TODO MixerState
    }

    public Object getState() {
        return null; // TODO MixerState
    }

    public void setState(Object state) throws ClassCastException {
        this.setLevel(((LayerState)state).getLevel());
    }

    public Mixable getLayer(int index) throws IndexOutOfBoundsException {
        return layers.get(index);
    }

    public Iterator<Mixable> iterator() {
        return layers.iterator();
    }

    public int size() {
        if(layers == null) {
            return 0;
        } else {
            return layers.size();
        }
    }

    /** For each child Mixable (e.g. Layer), draw the subscene and/or update the
     *  state of the child's (Layer's) animator given the current time. After
     *  animating, all children (Layers) will be ready to render their current
     *  state as Pixels.
     */
    public void animate(TimePoint timePoint) {
        for(Animator a: subscribers) {
            a.animate(timePoint);
        }
        if(level > 0.0) {
            for (Mixable layer : this) {
                if(layer.getLevel() > 0.0) {
                    layer.animate(timePoint);
                }
            }
        }
    }

    /** Mix the output of each child (Layer), starting with the background and
     *  ending with the foreground.
     */
    public void mixWith(Pixel[] otherPixels) {
        // Clear the canvas
        for(Pixel pixel: pixels) {
            pixel.setBlack();
        }

        // Draw all visible layers
        if(level > 0.0) {
            for(Mixable layer : this) {
                if(layer.getLevel() > 0.0) {
                    layer.mixWith(pixels);
                }
            }

            // Adjust brightness according to master fader
            if(level < 1.0) {
                float lf = (float) level;
                for(Pixel p: pixels) {
                    p.scale(lf);
                }
            }
        }
    }

    /** FIXME The Mixer normally runs in a separate thread, so it should make a
     * deep copy of the devices[], in case the user edits their associated
     * PatchSheet later, while the show is running. Also, it would be best if
     * Device were a read-only interface so that it could be safely shared
     * among Layers.
     */
    public void patchDevices(Device[] devices) {
        deviceCount = devices.length;
        for(Mixable layer : layers) {
            layer.patchDevices(devices);
        }

        // Initialize the reusable canvas.
        pixels = new Pixel[deviceCount];

        for(int i=0; i<pixels.length; i++) {
            pixels[i] = Pixel.black();
        }

    }

    /** See borrowing contract on Effect. */
    public Pixel[] render() {
        this.mixWith(pixels);
        return pixels;
    }

    public String toString() {
        return "Mixer";
    }

    public String getDescription() {
        StringBuffer sb = new StringBuffer();
        for(int i=0; i<layers.size(); i++) {
            Mixable layer = layers.get(i);
            sb.append("layer" + i + ": " + layer
                    + " @" + layer.getLevel()
                    + " (" + layer.getBlendOp() + " blend mode)\n");
        }
        return sb.toString();
    }

    /** A Mixer doesn't currently care when its parent Mixer's level changes. */
    public void levelChanged(double oldLevel, double newLevel) {}

    public List<StatefulAnimator> getSubscribers() {
        return subscribers;
    }

}
