(ns otoch.consts
  (:require [infinitelives.utils.vec2 :as vec2]))

(def h-edge 0.3)
(def minus-h-edge (- 1 h-edge))
(def v-edge 0.45)
(def minus-v-edge (- 1 v-edge))

(def jump-accel-1 0.1)
(def jump-accel-2+ 0.03)
(def jump-frames 10)

(def gravity (vec2/vec2 0 0.01))
(def blood-gravity (vec2/vec2 0 0.001))
