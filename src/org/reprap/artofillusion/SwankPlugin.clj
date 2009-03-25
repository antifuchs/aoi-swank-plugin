;; (require :artofillusion)

(ns org.reprap.artofillusion.SwankPlugin
  (:gen-class
   :implements [artofillusion.Plugin]
   :prefix "plugin-"
   :methods [[getVersion [] String]]))

(defn plugin-getVersion [this]
  "0.0")

(defn plugin-processMessage [this message args]
  nil)