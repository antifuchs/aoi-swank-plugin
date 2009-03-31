;;; Draw a pentagram as a polygon

(ns org.reprap.artofillusion.repl)

(defn draw-pentagram [inner-radius]
 (def-tree [(window) "HAIL SATAN"]
   (star 5 inner-radius
         ;; ratio between the inner and outer radius is 1 +
         ;; golden ratio, apparently.
         (* inner-radius (+ 1 (/ (+ 1 (Math/sqrt 5)) 2))))))

(defn draw-hexagram [inner-radius]
  (def-tree [(window) "6 points! Whee!"]
   (star 6 inner-radius
         ;; ratio between the inner and outer radius is 1 +
         ;; golden ratio, apparently.
         (* inner-radius (Math/sqrt 3)))))

(draw-pentagram 15)

(originate [(* 15 (+ 1 (/ (+ 1 (Math/sqrt 5)) 2))) 0 0]
 (draw-hexagram 10))