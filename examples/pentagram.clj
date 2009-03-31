;;; Draw a pentagram as a polygon

(ns org.reprap.artofillusion.repl)

(defn draw-pentagram [inner-radius]
 (def-tree [(window) "HAIL SATAN"]
   (star 5 inner-radius
         ;; ratio between the inner and outer radius is 1 +
         ;; golden ratio, apparently.
         (* inner-radius (+ 1 (/ (+ 1 (Math/sqrt 5)) 2))))))

(draw-pentagram 15)