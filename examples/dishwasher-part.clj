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

      gripradius 70
      gripoverlap 4

      indicator-radius 2]
  (def-tree [(window) "rotary-dial" {:cs [zradius (- zradius gripoverlap) 0]}]
    ;; The grip:
    (difference
     (cylinder zheight zradius zradius
               {:cs [0 0 (/ zheight 2)
                     90 0 0]})
     (cylinder (* 3 zheight) gripradius gripradius
               {:cs [0 (+ gripradius (- zradius gripoverlap)) 0
                     90 0 0]})
     (cylinder (* 3 zheight) gripradius gripradius
               {:cs [0 (- 0 gripradius (- zradius gripoverlap)) 0
                     90 0 0]})
     (cylinder (* 3 zheight) indicator-radius indicator-radius
               {:cs [zradius 0 0
                     90 0 0]}))
    (difference
     (union
      (cylinder (+ 2 plug-height) plug-outer-radius plug-outer-radius
                {:cs [0 0 (- (+ zheight (/ plug-height 2)) 1)
                      90 0 0]})
      (cylinder extra (* 2 plug-outer-radius) (* 2 plug-outer-radius)
                {:ratio 0
                 :cs [0 0 (- (+ zheight (/ extra 2)) 2.5)
                      -90 0 0]}))

     ;; Inner cross cut for fitting on the pin on the machine:
     (union
      (cylinder (+ plug-height 24) plug-inner-radius plug-inner-radius
                {:cs [0 0 (+ zheight (- (/ plug-height 2) 1))
                      90 0 0]})
      (cube (* 2 plug-attached-nub-extent) plug-attached-nub-width (+ 20 plug-height)
            {:cs [(- plug-inner-radius) 0 (+ zheight (- (/ plug-height 2) 1))]})
      (cube (* 2 plug-attached-nub-extent) plug-top-nub-width (+ 21 plug-height)
            {:cs [plug-inner-radius 0 (+ zheight (- (/ plug-height 2) 1))]})
      (cube plug-attached-nub-width (* 2 (+ plug-inner-radius plug-attached-nub-extent)) (+ 22 plug-height)
            {:cs [0 0 (+ zheight (- (/ plug-height 2) 1))]})))))