(ns org.reprap.artofillusion.SwankTool
  (:require swank.swank clojure.main)
  (:gen-class
   :implements [artofillusion.ModellingTool]
   :prefix "tool-"))

(defn tool-getName [this]
  "Start Swank REPL")

(defn tool-commandSelected [this window]
  (clojure.main/with-bindings 
   (swank.swank/ignore-protocol-version "2009-03-25") 
   (swank.swank/start-server "/dev/null" :port 4006 :encoding "iso-latin-1-unix")))