(ns org.reprap.artofillusion.objects
  (:refer-clojure)
  (:use clojure.contrib.seq-utils)
  (:import (artofillusion.object
            Cube
            Cylinder
            Sphere
            Curve
            Mesh
            TriangleMesh)
           (artofillusion
            UndoRecord)
           (artofillusion.math
            CoordinateSystem
            Vec3)
           (artofillusion.object
            ObjectInfo
            CSGObject)
           (artofillusion.tools
            ExtrudeTool)))

;;; Manipulating the scene itself:

(defn scene [window]
  (.getScene window))

;;; Manipulating the current selection:

(defn any-parent-selected? [object window]
  (loop [parent (.getParent object)]
    (cond
      (nil? parent) false
      (.isObjectSelected window parent) true
      true (recur (.getParent parent)))))

(defn current-selection
  "Returns a normalized selection, i.e. one without children of a selected 
parent."
  ([window]
     (loop [selection (.getSelectedObjects window)
            normalized-selection []]
       (let [current-object (first selection)]
         (cond
           (nil? current-object) normalized-selection
           (any-parent-selected? current-object window) (recur (rest selection)
                                                               normalized-selection)
           true (recur (rest selection)
                       (conj normalized-selection current-object)))))))

(defmacro modifying-object-tree [window & body]
  `(do
     ~@body
     (.updateImage ~window)
     (.updateMenus ~window)))

(defn replace-with-object [replacement object window]
  (let [texture (.getTexture (.object object))
        texture-map (.getTextureMapping (.object object))]
    (modifying-object-tree window
      (.setObject object replacement)
      (.setTexture replacement texture texture-map)
      (.clearCachedMeshes object))))

;;; Undo handling:

(defonce *current-undo-record* nil)

(defmacro with-undo-record [window & body]
  `(if (nil? *current-undo-record*)
     (binding [*current-undo-record* (new UndoRecord ~window false)]
       (let [value# (do ~@body)]
         (.setUndoRecord ~window *current-undo-record*)
         value#))
     (do ~@body)))

;;; Adding an object to a window:

(defn object-name [object]
  (print-str object))

(def *current-origin* (new Vec3 0 0 0))
(def *current-orientation* (new Vec3 0 0 0))

(defn make-cs
  ([]
     (make-cs :relative 0 0 0 0 0 0))
  ([x y z]
     (make-cs :relative x y z))
  ([kind x y z]
     (make-cs kind x y z 0 0 0))
  ([x y z
    orientation-x orientation-y orientation-z]
     (make-cs :relative x y z orientation-x orientation-y orientation-z))
  ([kind
    x y z
    orientation-x orientation-y orientation-z]
     (condp = kind
       :relative (let [origin (.plus *current-origin* (new Vec3 x y z))
                       orient (.plus *current-orientation*
                                     (new Vec3 orientation-x orientation-y orientation-z))]
                   (make-cs :absolute (.x origin) (.y origin) (.z origin)
                            (.x orient) (.y orient) (.z orient)))
       :absolute (let [cs (new CoordinateSystem)]
                   (.setOrigin cs (new Vec3 x y z))
                   (.setOrientation cs orientation-x orientation-y orientation-z)
                   cs))))

(defn vec3-coords [vec]
  [(.x vec) (.y vec) (.z vec)])

(defn object*
  "Make an AoI ObjectInfo object from a 3dObject or CSGObject or another ObjectInfo."
  ([object]
     (if (isa? (class object) ObjectInfo)
       object
       (object* object {})))
  ([object keys]
     (new ObjectInfo object
          (apply make-cs
                 (if-let [cs (get keys :cs)]
                   cs
                   []))
          (or (get keys :name) (object-name object)))))

(defn adjust-object* [object keys]
  (if-let [cs-raw (get keys :cs)]
    (.setCoords object (apply make-cs cs-raw)))
  (if-let [name (get keys :name)]
    (.setName object name))
  object)

(defn add-object [object-info window]
  (with-undo-record window
    (.addObject window object-info *current-undo-record*)
    object-info))

;;; Finding objects:

(defn find-object [name window]
  (if (isa? (class name) ObjectInfo)
    name
    (.getObject (scene window) name)))

(defn position-of-object [name window]
  (.indexOf (scene window) (find-object name window)))

;;; Removing objects:

(defn delete-object [name window]
  (with-undo-record window
    (if-let [object (find-object name window)]
      (do
        (doseq [child (.getChildren object)]
          (delete-object child window))
        (.removeObject window (position-of-object object window)
                       *current-undo-record*)))))

;;; Transformations:

(defn invoke-with-origin [origin-vec reset-origin? fn]
  (binding [*current-origin* (if reset-origin?
                               origin-vec
                               (.plus *current-origin* origin-vec))]
    (fn)))

(defn invoke-with-orientation [vec reset-orientation? fn]
  (binding [*current-orientation* (if reset-orientation?
                                    vec
                                    (.plus *current-orientation* vec))]
     (fn)))

(defmacro originate [[x y z & reset-origin?] & body]
  `(invoke-with-origin (new Vec3 ~x ~y ~z)
                       ~(first reset-origin?)
                       (fn [] [~@body])))

(defmacro orient [[x y z & reset-orientation?] & body]
  `(invoke-with-orientation (new Vec3 ~x ~y ~z)
                            ~(first reset-orientation?)
                            (fn [] [~@body])))

(defmacro transforming [[[& origin] [& orientation]] & body]
  (let [origin-body (if origin
                      `((originate [~@origin] ~@body))
                      body)]
    (let [orientation-body (if orientation
                             `(orient [~@orientation] ~@origin-body)
                             (first origin-body))]
      orientation-body)))

;;; Creating new objects:

(defonce *csg-translations* {:union (CSGObject/UNION)
                             :intersection (CSGObject/INTERSECTION)
                             :difference (CSGObject/DIFFERENCE12)})

(defn csg-object [operation objects]
  (let [operation (get *csg-translations* operation)
        objects (flatten objects)]
    (loop [csg (new CSGObject
                    (first objects)
                    (second objects)
                    operation)
           objects (nthnext objects 2)]
      (if (empty? objects)
        (object* csg {:cs [:absolute 0 0 0 0 0 0]})
        (recur (new CSGObject
                    (object* csg {:cs [:absolute 0 0 0 0 0 0]})
                    (first objects)
                    operation)
               (rest objects))))))

(defn split-arglist [arglist]
  (split-with (complement keyword?) arglist))

(defmacro object-info-ify [[arglist params] & body]
  `(let [[~arglist objinfo-arglist#] (split-arglist ~params)]
     (let [object-3d# (do ~@body)]
       (object* object-3d# (apply hash-map objinfo-arglist#)))))

(defn union [& rest]
  (csg-object :union rest))

(defn intersection [& rest]
  (csg-object :intersection rest))

(defn difference [& rest]
  (csg-object :difference rest))

;;; Primitive 3D objects:

(defn cube [& x-y-z-and-objinfo-args]
  (object-info-ify [[x y z] x-y-z-and-objinfo-args]
    (new Cube x y z)))

(defn cylinder [& height-xradius-zradius-ratio-and-keys]
  (object-info-ify [[height xradius yradius & ratio-rest]
                    height-xradius-zradius-ratio-and-keys]
    (let [ratio (condp = (count (seq ratio-rest))
                  1 (first ratio-rest)
                  0 1.0)]
      (new Cylinder height xradius yradius ratio))))

(defn sphere [& x-y-zradius-and-keys]
  (object-info-ify [[xradius yradius zradius] x-y-zradius-and-keys]
    (new Sphere xradius yradius zradius)))

;;; Primitive polygons:

(defn vectors-curve [vectors closed]
  (let [smooth-array (make-array Float/TYPE (count vectors))]
    (new Curve
         (into-array Vec3 vectors)
         smooth-array
         Mesh/NO_SMOOTHING
         closed)))

(defn star [& n-inner-outer-and-keys]
  (object-info-ify [[n inner outer] n-inner-outer-and-keys]
    (loop [index 0
           vectors []]
      (if (< index (* 2 n))
        (let [inner-vector (new Vec3
                                (Math/cos (* Math/PI (/ index n)))
                                (Math/sin (* Math/PI (/ index n)))
                                0)
              outer-vector (new Vec3
                                (Math/cos (* Math/PI (/ (+ index 1) n)))
                                (Math/sin (* Math/PI (/ (+ index 1) n)))
                                0)]
          (. inner-vector scale inner)
          (. outer-vector scale outer)
          (recur (+ index 2)
                 (concat vectors [inner-vector outer-vector])))
        (vectors-curve vectors true)))))

(defn roll [& n-big-small-small2]
  (object-info-ify [[n big small small2] n-big-small-small2]
    (let [small (or small 0.5)
          small2 (or small2 0.4)]
      (loop [index 0
             vectors []]
        (let [big-a (/ (* 2 Math/PI index) n)
              len (* big big-a)
              small-a (/ len small)]
          (if (< index n)
            (recur (+ index 1)
                   (conj vectors
                         (new Vec3
                              (+ (* (+ big small) (Math/cos big-a))
                                 (* small2 (Math/cos small-a)))
                              (+ (* (+ big small) (Math/sin big-a))
                                 (* small2 (Math/sin small-a)))
                              0)))
            (vectors-curve vectors true)))))))

(defn circular-polygon [& n-radiusx-radiusy]
  (object-info-ify [[n radius-x radius-y] n-radiusx-radiusy]
    (loop [index 0
           vectors []]
      (if (< index n)
        (recur (+ index 1)
               (conj vectors
                     (new Vec3
                          (* radius-x (Math/cos (* 2 Math/PI (/ index n))))
                          (* radius-y (Math/sin (* 2 Math/PI (/ index n))))
                          0)))
        (vectors-curve vectors true)))))

;;; Extruding polygons:

(defn vector* [x y z]
  (new Vec3 x y z))

(defn extrude [& object-segments-twist-direction]
  (object-info-ify [[object direction segments twist]
                    object-segments-twist-direction]
    (let [twist (or twist 0.0)
          direction (apply vector* direction)
          object-3d (if (isa? (class (.getObject object)) TriangleMesh)
                      (.getObject object)
                      (.convertToTriangleMesh (.getObject object) 0.1))
          smooth (make-array Float/TYPE segments)]
      (if object-3d
        (let [vectors (loop [index 0
                             vectors []]
                        (if (< index segments)
                          (let [vector (new Vec3 direction)]
                            (aset smooth index (float 1.0))
                            (.scale vector (/ index segments))
                            (recur (+ 1 index)
                                   (conj vectors vector)))
                          vectors))]
          (ExtrudeTool/extrudeMesh object-3d
                                   (new Curve
                                        (into-array Vec3 vectors)
                                        smooth Mesh/APPROXIMATING
                                        false)
                                   (.getCoords object)
                                   (make-cs :absolute 0 0 0 0 0 0)
                                   (* twist (/ Math/PI 180))
                                   true))))))


;;; A surface syntax for specifying object trees:

(defn make-tree [keys & root-objects]
  (let [root-objects (clojure.contrib.seq-utils/flatten root-objects)]
   (if (> (count root-objects) 1)
     (adjust-object* (apply union root-objects) keys)
     (adjust-object* (first root-objects) keys))))

(defmacro def-tree [[window name] & body]
  `(let [window# ~window
         name# ~name]
     (delete-object name# window#)
     (add-object (make-tree {:name ~name} ~@body) window#)))