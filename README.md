udder 0.4.1-pre
===============

HTTP server, animators, mixer, and Open Pixel Control (OPC) renderer for a 2014 public lighting installation at Boulder's Dairy Center for the Arts.


Author
------
[Mike Bissell, Coil Lighting](http://www.coil-lighting.com/)


Getting Started
---------------

You need to install JDK 1.7+ and Maven 3.2.3+.

To help bootstrap development, this repository temporarily comes with a copy of Maven 3.2.3 plus several jars required by the application. Eventually these resources will disappear. If you've already installed the JDK and Maven on your devbox, you don't have to use them. Mac OS X users may `source env.sh` to get set up rapidly.

Once Maven and the JDK are installed in your current environment (see `env.sh` for examples), `cd udder` and run either the `build` script or `build_clean`.

After the build script succeeds, you may start the server using the `serve_from_build` script in the same directory. By default the server listens on [http://localhost:8080](http://localhost:8080) and attempts to render at 100fps max.

Alternately, you should be able to serve from the dist jar using the neighboring `serve_from_dist` script, if your environment is set up. (More on this in a few days.)


Udder Service Architecture in a Nutshell
----------------------------------------

The class com.coillighting.udder.ServicePipeline assembles the application components into a webserver capable of driving OPC RGB pixel lighting instruments. The coarse grained pipeline has three stages, with network IO:

Network => HttpServiceContainer => ShowRunner => OpcTransmitter(s) => Network => Lights

Details:
* A human **lighting designer** initiates HTTP **requests** from a web page in a browser.
* A SimpleFramework (v5.1.5) server listens for incoming HTTP **requests**. Request **payloads** are curently expected to be JSON structures.
* A Boon (v0.23) JSON decoder converts each request **payload** into a **command** object.
* **Commands** are inserted into a concurrent queue, the **command queue**.
* In one separate thread, a ShowRunner object runs a periodic event loop, one event per frame. In each frame, **commands** are drained from the **command queue** and processed.
* A **command** normally mutates the state of the ShowRunner or one of its children (the Mixer, an Animator, or the Renderer).
* When there are no more commands to process, the ShowRunner animates, composites, and renders the current **frame**.
* The current **frame** is then inserted into a second concurrent queue, the **output queue**.
* In another separate thread, an OpcTransmitter object blocks until the ShowRunner sends it a new **frame** via the **output queue**. Upon the arrival of a new frame, the OpcTransmitter transmits the frame via TCP/IP to a remote OPC listener.
* The remote OPC listener (normally a stock OPC daemon process) writes the contents of the incoming **frame** to downstream RGB **pixel devices**, normally an array of LED strips.
* **Pixel devices** emit **photons**.
* **Photons** enter the eyes of the audience and of the **lighting designer**. Thus the cycle is complete.

Important points:
* Udder has a **multithreaded** architecture, coupled by two concurrent queues.
* Data flows down **one** non-branching path, through the three linked stages of the pipeline.
* The ShowRunner processes commands and renders frames **asynchronously** with respect to incoming requests.
* The OpcTransmitter broadcasts frames **asynchronously** with respect to the renderer.
* You may optionally connect multiple output queues to multiple OpcTransmitters, to copy the show to multiple destinations independently. We used this to stream frames to both physical LED strips and a 3D visualizer simultaneously. Each transmitter runs in its own thread, with its own timing parameters.


Udder Animation Architecture in a Nutshell
------------------------------------------

The class com.coillighting.mix.Mixer implements the root of a scene. Everything creative is the responsibility of your Mixer and its children, including animation, compositing (blending), cross-fading, oscillation, step sequencing, and looping.

Details:
* You may run multiple, independent scenes. Just implement more than one unrelated Mixer. A scene is effectively just a root Mixer and its directed, acyclic subgraph of children.
* Each Mixer holds a tree Mixable objects.
* The most common Mixable object is a Layer. A typical Mixer blends together several Layers.
* Layers are adapters for Effects. The Layer class works with the Mixer class to supply essential services like blending, brightness control, and timing signal propagation. This leaves your Effects free to focus on the graphical details specific to each Effect.
* An Effect is plugged into each Layer.
* In theory, a Mixer is Mixable, and so you can compose multiple Mixers with multiple layers into a tree of submixers and sublayers. This feature is still unstable.
* In theory, an Effect is efficiently reusable in multiple layers, and even in multiple (otherwise independent) scenes. This feature likewise remains unstable.
* Eventually we plan to support cyclical scenes, for efficient feedback (with 1 frame delay over reentrant paths). This feature is still a pipe dream.

Important points:
* Udder's design philosophy is to assume responsibility for all of the generic aspects of scene construction so that you can just focus on the art.
* Udder is a modular toolkit, not a monolithic application. Virtually everything in Udder is written without global variables. Dependencies are always "injected," normally as constructor arguments. This means you can construct very complex spaghetti scenes by connecting prefab Udder components like Legos.

In short, if you want to make Udder draw something new, just write an Effect. The rest is provided. Every once in a while you'll want a new crossfade contour, or a new LFO waveform, or perhaps a new blend mode, and then you'll need to contribute to the core classes.

A good way to quickly build a complex scene is to make one Effect with a few variables, then load your Effect (with variations) into many Layers belonging to your scene's root Mixer.


Dependency Links
----------------
Apache Maven:
* [Maven - official website](http://maven.apache.org/)
* [Maven - source repository](https://git-wip-us.apache.org/repos/asf?p=maven.git)

The Simple webserver:
* [Simple - official website](http://www.simpleframework.org/)
* [Simple - repository](http://sourceforge.net/projects/simpleweb/)

Boon, which we include for its JSON support:
* [Boon - tutorial (beware: some docs may be out of date)](https://github.com/RichardHightower/boon/wiki)
* [Boon - repository](https://github.com/RichardHightower/boon)

Open Pixel Control (OPC), our chosen network protocol for lighting control:
* [OPC - official website](http://openpixelcontrol.org/)
* [OPC - repository](https://github.com/zestyping/openpixelcontrol)

Micah Scott's Fade Candy device, the LED pixel driver we use:
* [FadeCandy - repository](https://github.com/scanlime/fadecandy)
* [FadeCandy - Sparkfun catalog page](https://www.sparkfun.com/products/12821)
* [FadeCandy - Adafruit catalog page](http://www.adafruit.com/products/1689)

The official API Javadocs for certain "special sauce" coupler components:
* [java.util.concurrent.ConcurrentLinkedQueue](http://docs.oracle.com/javase/7/docs/api/index.html?java/util/concurrent/ConcurrentLinkedQueue.html)
* [java.util.concurrent.LinkedBlockingQueue](http://docs.oracle.com/javase/7/docs/api/index.html?java/util/concurrent/LinkedBlockingQueue.html)

Eric Miller's 3D simulator (currently a Chrome extension) and visualizer for the Dairy installation:
* [Archway - Github](https://github.com/patternleaf/archway)

3rd party libraries are covered by their own licenses (mostly Apache 2.0, MIT, or equivalent). Everything else in this repository is released under the following license:


Wishlist
--------
* Udder is a Java port of LD50, a circa 2005 Objective-C app by the same author. We need to port certain useful Effects from LD50 -- at least the Color Organism and Mister Stroboto.
* Continue to expand docs.
* 100% coverage Junit tests.
* Separate the Boulder Dairy scene into its own app. Document this example with photos when available.
* For the Dairy scene, document the connection with Eric's in-browser visualizer.
* MIDI input for Mixer control (easy port from LD50).
* MIDI input for Effect modulation.
* MIDI output.
* DMX input for high-level Mixer control.
* DMX input for Effect modulation.
* DMX output (easy port from LD50 or Libsinuous).
* OSC input for Mixer control.
* OSC input for Effect modulation.
* OSC output.
* MIDI and OSC Motorboard / LEDboard control for Mixer trees (not easy to be generic, but there is a reference impl in LD50).
* Bspline, NURBS, and Catmull-Rom spline signal generators, envelopes, and crossfaders (easy port from LD50).
* A self-assembling web UI for Mixer control and Effect modulation.


Acknowledgements
----------------
Special thanks to the following LDs, engineers, and suppliers for contributing
concepts, bug reports, parts, funds, visual designs, bug fixes, hands-on testing,
and tooling to this project (including its earlier incarnation as LD50):

* Allison Vanderslice
* Becky Vanderslice
* Chris Macklin
* Dan Cohn
* Dan Julio
* Dave Able
* Ed Colmar
* Eric Miller
* Erin Rosenthal
* Goolie Gould
* Josh Erickson
* Jordan K. "Janitor" Paul
* [Sparkfun.com](http://www.sparkfun.com)
* ...and all the loyal beta testers from RA, Vox Lux, and Invisible Photons.


Apache License, v2.0
--------------------

Copyright 2014 Michael Bissell

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
