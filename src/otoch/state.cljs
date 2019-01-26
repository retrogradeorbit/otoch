(ns otoch.state
  (:require [infinitelives.utils.vec2 :as vec2]))

(defonce state
  (atom {:pos (vec2/vec2 0 0)}))
