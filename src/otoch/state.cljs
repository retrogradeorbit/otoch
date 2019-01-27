(ns otoch.state
  (:require [infinitelives.utils.vec2 :as vec2]))

(def initial-state
  {:pos (vec2/vec2 0 0)
   :touched-heart? false})

(defonce state
  (atom initial-state))

(defn reset-state! []
  (reset! state initial-state))
