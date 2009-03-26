;;; Copyright (c) Andreas Fuchs. All rights reserved.
;;; The use and distribution terms for this software are covered by the
;;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;; which can be found in the file epl-v10.html at the root of this distribution.
;;; By using this software in any fashion, you are agreeing to be bound by
;;; the terms of this license.
;;; You must not remove this notice, or any other, from this software.

(ns org.reprap.artofillusion.repl
  (:require clojure))

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

