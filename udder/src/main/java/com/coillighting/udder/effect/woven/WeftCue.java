package com.coillighting.udder.effect.woven;

import com.coillighting.udder.Pixel;
import com.coillighting.udder.TimePoint;
import com.coillighting.udder.blend.BlendOp;
import com.coillighting.udder.blend.MaxBlendOp;

public class WeftCue extends CueBase {

    protected int weftX = 0;
    protected int weftY = 0;

    protected long stepDuration = 0;
    protected long stepStartTime = 0;

    protected Pixel threadColor = null;
    protected Pixel cursorColor = null;
    protected Pixel backgroundColor = null;

    protected BlendOp blendOp = null;


    public WeftCue(long duration, WovenFrame frame) {
        super(duration, frame);

        // TODO variable colors
        this.threadColor = new Pixel(1.0f, 0.0f, 0.0f);
        this.cursorColor = Pixel.white();
        this.backgroundColor = Pixel.black();
        this.blendOp = new MaxBlendOp();
    }

    public void startStepTimer(TimePoint timePoint) {
        stepStartTime = timePoint.sceneTimeMillis();
        stepDuration = (long)(this.getDuration()
            / (2.0 * (double)frame.weft[0].length));
    }

    public void animate(TimePoint timePoint) {
        if(fadeState == CueFadeStateEnum.START) {
            weftX = 0;
            weftY = 0;
            this.startTimer(timePoint);
            this.startStepTimer(timePoint);

            // Clear the canvas
            for(int x=0; x<2; x++) {
                for(Pixel p: frame.weft[x]) {
                    p.setColor(backgroundColor);
                }
            }
        } else if(this.isElapsed(timePoint)) {
            // Finish the last step in this cue, prepare to move to the next cue.
            Pixel p = frame.weft[1][frame.weft[1].length - 1];
            p.setColor(threadColor);
            this.stopTimer();
            return;
        }

        double elapsed = CueBase.computeFractionElapsed(timePoint,
            stepStartTime, stepDuration);
        if(elapsed >= 1.0) {
            if(weftX == 0) {
                // Finish this step, move right to the next step.
                Pixel p = frame.weft[weftX][weftY];
                p.setColor(1.0f, 1.0f, 1.0f); // TODO color selection
                weftX = 1;
            } else if(weftY + 1 >= frame.weft[0].length) {
                return;
            } else {
                // Finish this step, move up to the next step in this cue.
                Pixel p = frame.weft[weftX][weftY];
                p.setColor(threadColor); // TODO color selection

                // TODO: blank lines between threads
                // TODO: variable ratio of thread width to blank line width?
                weftY += 1;
                weftX = 0;
            }
            this.startStepTimer(timePoint);
        }
        // TODO nonlinear fade-in, poss. nonlinear cursor fade
        float brightness = (float) elapsed;

        // TODO reuse objects?
        Pixel color = new Pixel(threadColor);

        // Fade from white to threadColor as we fade in.
        color.blendWith(cursorColor, 1.0f - brightness, blendOp);

        // Fade in from black
        color.scale(brightness);

        // TODO nonlinear fade in
        frame.weft[weftX][weftY].setColor(color);
    }

}
