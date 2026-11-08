(ns build
  (:require [top.kzre.clj-build-template.simple :as tpl]))

(def params {:template (tpl/create-template)
             :artifact "krro-d3"
             :compile-java? true
             :compile-clj? true
             :version "0.1.0"})

(defn clean [_]
      (tpl/clean))

(defn compile-java [_]
      (tpl/compile-java params))

(defn compile-clj [_]
      (tpl/compile-clj params))

(defn jar [_]
      (tpl/jar params))

(defn install [_]
      (tpl/install params))