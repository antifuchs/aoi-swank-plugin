(ns org.reprap.artofillusion.repl)

(let [zradius 45/2
      zheight 10
      plug-height 20
      plug-outer-radius 7
      plug-inner-radius 3.625
      plug-attached-nub-width 3.7
      plug-attached-nub-extent 1.25
      plug-top-nub-width 2.3
      extra 7.5

      grip-radius 70
      grip-overlap 4

      indicator-radius 2]
  (def-tree [(window) "rotary-dial"]
    (originate [(+ zradius (/ zradius 2)) zradius 0]
      ;; The grip:
      (difference
       (originate [0 0 (+ (/ zheight 2))]
         (orient [90 0 0]  ; We need to rotate cylinders to grow into the Z axis.
           (cylinder zheight zradius zradius)
           (cylinder (* 3 zheight) grip-radius grip-radius
                     {:cs [0 (+ grip-radius (- zradius grip-overlap)) 0]})
           (cylinder (* 3 zheight) grip-radius grip-radius
                     {:cs [0 (- 0 grip-radius (- zradius grip-overlap)) 0]})
           (cylinder (* 3 zheight) indicator-radius indicator-radius
                     {:cs [zradius 0 0]}))))

      ;; The plug itself
      (originate [0 0 (+ (+ 0 zheight (/ plug-height 2)) 1)]
        (orient [-90 0 0]
          (difference
           (union
             (cylinder (+ 2 plug-height) plug-outer-radius plug-outer-radius)
             (cylinder extra (* 2 plug-outer-radius) (* 2 plug-outer-radius)
                       {:ratio 0
                        :cs [0 0 (+ (- (/ plug-height 2)) (/ extra 4))]}))
           (cylinder (+ plug-height 24) plug-inner-radius plug-inner-radius)
           (orient [-90 0 0]
             (cube (* 2 plug-attached-nub-extent) plug-attached-nub-width (+ 20 plug-height)
                   {:cs [(- plug-inner-radius) 0 0]})
             (cube (* 2 plug-attached-nub-extent) plug-top-nub-width (+ 21 plug-height)
                   {:cs [plug-inner-radius 0 0]})
             (cube plug-attached-nub-width
                   (* 2 (+ plug-inner-radius plug-attached-nub-extent))
                   (+ 22 plug-height)))))))))