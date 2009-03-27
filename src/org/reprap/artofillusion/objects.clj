(ns org.reprap.artofillusion.objects
  (:refer-clojure)
  (:import (artofillusion.object
            Cube
            Cylinder)
           (artofillusion
            UndoRecord)
           (artofillusion.math
            CoordinateSystem)
           (artofillusion.object
            ObjectInfo
            CSGObject)))

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

(defn make-cs
  ([] (new CoordinateSystem))
  ([x y z] (doto (new CoordinateSystem)
             (.setOrientation x y z))))

(defn objspec-object-info
  ([object]
     (if (isa? (class object) ObjectInfo)
       object
       (objspec-object-info object (object-name object) (make-cs))))
  ([object name-or-cs]
     (if (isa? (class name-or-cs) String)
       (objspec-object-info object name-or-cs (make-cs))
       (objspec-object-info object (object-name object)
                            (apply make-cs name-or-cs))))
  ([object name cs]
     (new ObjectInfo object cs name))
  ([object name x y z]
     (new ObjectInfo object (make-cs x y z) name)))

(defn add-object [object-info window]
  (with-undo-record window
    (.addObject window object-info *current-undo-record*)
    object-info))

(defn add-objects [window & objspecs]
  (with-undo-record window
    (for [objspec objspecs]
      (add-object (apply objspec-object-info objspec) window))))

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

;;; Creating new objects:

(defonce *csg-translations* {:union (CSGObject/UNION)
                             :intersection (CSGObject/INTERSECTION)
                             :difference (CSGObject/DIFFERENCE12)})

(defn csg-object [operation object1 object2 & objects]
  (let [operation (get *csg-translations* operation)
        csg (new CSGObject
                 (objspec-object-info object1)
                 (objspec-object-info object2)
                 operation)]
    (loop [objects objects
           csg csg]
      (if (empty? objects)
        (objspec-object-info csg)
        (recur (rest objects)
               (new CSGObject
                    (objspec-object-info csg)
                    (objspec-object-info (first objects))
                    operation))))))

(defn union [object1 object2 & rest]
  (apply csg-object :union object1 object2 rest))

(defn intersection [object1 object2 & rest]
  (apply csg-object :intersection object1 object2 rest))

(defn difference [object1 object2 & rest]
  (apply csg-object :difference object1 object2 rest))

(defn cube [x y z]
  (new Cube x y z))

(defn cylinder
  ([x y z] (cylinder x y z 1))
  ([x y z ratio] (new Cylinder x y z ratio)))

