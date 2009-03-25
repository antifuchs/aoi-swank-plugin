This is a simple plugin for Art of Illusion. Also, it serves as an example for how to embed clojure in a large java system.

= How to build =

You'll need:

* Art of Illusion
* Emacs and these packages:
** Slime (http://common-lisp.net/project/slime)
** Clojure-mode (http://github.com/jochu/clojure-mode)
** Swank-clojure (http://github.com/jochu/swank-clojure)

To build the plugin, follow these steps:

1. Download Art of Illusion and install it somewhere.
1. Edit build.properties to reflect the directory where you installed Art of Illusion. There should be a file called aoidir/ArtOfIllusion.jar.
1. Type "ant" on the root directory and you'll get a file called Plugins/SwankRepl.jar
1. Symlink this file to your Art of Illusion Plugins/ dir.
1. Start AoI, and in Tools, select "Start Swank REPL". This will open a swank listener port on localhost:4006. You can M-x slime-connect to this port from emacs and then drive Art of Illusion. Yay!