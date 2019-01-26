(ns otoch.particle
  (:require [infinitelives.utils.vec2 :as vec2]
            [infinitelives.utils.events :as e]
            [infinitelives.utils.boid :as b]
            [infinitelives.utils.math :as math]
            [infinitelives.utils.spatial :as spatial]
            [infinitelives.utils.sound :as sound]
            [infinitelives.utils.console :refer [log]]
            [infinitelives.pixi.sprite :as s]
            [infinitelives.pixi.canvas :as c]
            [otoch.state :as state]
            [otoch.consts :as consts]
            [otoch.constraints :as constraints]
            [otoch.platforms :as platforms]
            [cljs.core.async :refer [<! timeout]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn spawn [container texture start-frame start-pos start-vel ω]
  (go
    (c/with-sprite container
      [particle (s/make-sprite texture :pos (vec2/scale start-pos 64))]
      (loop [n 0
             p start-pos
             v start-vel
             θ 0
             ω ω
             ]
        (s/set-pos! particle (vec2/scale p 64))
        (s/set-rotation! particle θ)
        (<! (e/next-frame))
        (when (< n 600)
          ;; still alive
          (let [platform-state
                (-> platforms/platforms
                    (platforms/prepare-platforms n)
                    (platforms/filter-platforms (vec2/zero)))

                new-pos
                (constraints/constrain-pos
                 constraints/particle-constrain
                 (-> platforms/platforms
                     (platforms/prepare-platforms (+ 1 start-frame n))
                     (platforms/filter-platforms p))
                 p (vec2/add p v))

                new-vel (-> new-pos
                            (vec2/sub p)

                            (vec2/add consts/blood-gravity)
                            (vec2/scale 0.99))

                ;; to see if weve landed, we try and move the particle down a bit
                ;; constrain it and see if they are the same place
                lower-pos (vec2/add new-pos (vec2/vec2 0 0.1))
                con-lower (constraints/constrain-pos
                           constraints/particle-constrain
                           (-> platforms/platforms
                               (platforms/prepare-platforms (+ 1 start-frame n))
                               (platforms/filter-platforms p))
                           new-pos lower-pos)

                hit-ground? (> (vec2/magnitude-squared (vec2/sub lower-pos con-lower)) 0.001)
                vel-x (vec2/get-x new-vel)
                ]

            (recur (inc n)
                   new-pos
                   new-vel
                   (+ θ ω)
                   (if hit-ground? 0 ω))))))))
