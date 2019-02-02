(ns otoch.enemy
  (:require [infinitelives.utils.vec2 :as vec2]
            [infinitelives.utils.events :as e]
            [infinitelives.utils.boid :as b]
            [infinitelives.utils.math :as math]
            [infinitelives.utils.spatial :as spatial]
            [infinitelives.utils.sound :as sound]
            [infinitelives.utils.async :as async]
            [infinitelives.utils.console :refer [log]]
            [infinitelives.pixi.sprite :as s]
            [infinitelives.pixi.canvas :as c]
            [otoch.state :as state]
            [otoch.consts :as consts]
            [otoch.constraints :as constraints]
            [otoch.platforms :as platforms]
            [otoch.map :as tm]
            [cljs.core.async :refer [<! timeout]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defonce enemies (atom {}))

#_ [@enemies @state/state]

(defn add! [ekey enemy]
  ;;(js/console.log "add!" ekey (str enemy))
  (swap! enemies assoc ekey enemy))

(defn remove! [ekey]
  ;;(js/console.log "remove!" ekey)
  (swap! enemies dissoc ekey))

(defn count-enemies []
  (count @enemies))

(defn collided?
  "given the players pos, find the first collided enemy"
  [player-pos]
  (let [coll (->>
              (for [[k {:keys [pos]}] @enemies]
                (let [distance-squared (vec2/magnitude-squared (vec2/sub player-pos pos))]
                  (when (< distance-squared 0.5)
                    k)))
              (filter identity)
              first)]
    coll
    ))

(def reverse-dir
  {:left :right
   :right :left})

(def speed 0.0005)

(defn sin [a t n]
  (* a (Math/sin (/ n t)))
  )

(defn cos [a t n]
  (* a (Math/cos (/ n t)))
  )

(defn wobble [y n]
  (+ y (* 4 (Math/sin (/ n 12))))
  )

(defmulti spawn (fn [_ _ sprite] sprite))

(defmethod spawn :enemy-0 [container start-pos sprite]
  (let [start-game-num (:game-num @state/state)
        ekey (keyword (gensym))]
    (go
      (async/continue-while
       (= start-game-num (:game-num @state/state))
       (let [start-frame 0]
         (c/with-sprite container
           [enemy (s/make-sprite sprite :pos (vec2/scale start-pos 64))]
           (add! ekey {:pos start-pos} )
           (loop [n 0
                  p start-pos
                  v (vec2/zero)
                  facing :left
                  ]
             (let [ppos (vec2/scale p 64)
                   [x y] ppos
                   y (wobble y n)
                   ]
               (swap! enemies assoc-in [ekey :pos] p)
               (s/set-pos! enemy x y))

             ;; stop processing when far away rom player
             #_ (while (>
                     (vec2/magnitude-squared
                      (vec2/sub (:pos @state/state) p))
                     (* 50 50))
               ;; far away from player. sleep for a bit (less CPU)
               (<! (e/wait-time (int (+ 1000 (* 1000 (rand)))))))

             (s/set-scale! enemy (if (= :left facing) 1 -1) 1)
             (<! (e/next-frame))
             (if true ;;(< n 3000)
               ;; still alive
               (do
                 #_ (let [frame (int (/ n 60))
                          texture :rune-1]
                      (s/set-texture! sprite (t/get-texture texture)))

                 (let [platform-state
                       (-> platforms/platforms
                           (platforms/prepare-platforms n)
                           (platforms/filter-platforms (vec2/zero)))

                       new-pos
                       (constraints/constrain-pos
                        constraints/enemy-constrain
                        (-> platforms/platforms
                            (platforms/prepare-platforms (+ 1 start-frame n))
                            (platforms/filter-platforms p))
                        p (vec2/add (vec2/add p v) (vec2/vec2 (if (= :left facing) (- speed) speed) 0)))

                       new-vel (-> new-pos
                                   (vec2/sub p)

                                   (vec2/add consts/gravity)
                                   (vec2/scale 0.98))
                       vel-x (vec2/get-x new-vel)
                       new-facing (if (zero? vel-x) (reverse-dir facing) facing)
                       ]

                   (recur (inc n)
                          new-pos
                          new-vel
                          new-facing)))
               p)))))
      (remove! ekey))))

(defmethod spawn :enemy-1 [container start-pos sprite]
  (let [start-game-num (:game-num @state/state)
        ekey (keyword (gensym))]
    (go
      (async/continue-while
       (= start-game-num (:game-num @state/state))
       (let [start-frame 0]
         (c/with-sprite container
           [enemy (s/make-sprite sprite :pos (vec2/scale start-pos 64))]
           (add! ekey {:pos start-pos} )
           (loop [n 0
                  p start-pos
                  v (vec2/zero)
                  facing :left
                  ]
             (let [ppos (vec2/scale p 64)
                   [x y] ppos
                   ]
               (swap! enemies assoc-in [ekey :pos] p)
               (s/set-pos! enemy x y))

             ;; stop processing when far away from player
             #_ (while (>
                     (vec2/magnitude-squared
                      (vec2/sub (:pos @state/state) p))
                     (* 50 50))
               ;; far away from player. sleep for a bit (less CPU)
               (<! (e/wait-time (int (+ 1000 (* 1000 (rand)))))))

             (s/set-scale! enemy (if (= :left facing) 1 -1) 1)
             (<! (e/next-frame))
             (if true ;;(< n 3000)
               ;; still alive
               (do
                 #_ (let [frame (int (/ n 60))
                          texture :rune-1]
                      (s/set-texture! sprite (t/get-texture texture)))

                 (let [platform-state
                       (-> platforms/platforms
                           (platforms/prepare-platforms n)
                           (platforms/filter-platforms (vec2/zero)))

                       new-pos
                       (constraints/constrain-pos
                        constraints/enemy-constrain
                        (-> platforms/platforms
                            (platforms/prepare-platforms (+ 1 start-frame n))
                            (platforms/filter-platforms p))
                        p (vec2/add (vec2/add p v) (vec2/vec2 (if (= :left facing) (- speed) speed) 0)))

                       new-vel (-> new-pos
                                   (vec2/sub p)

                                   (vec2/add consts/gravity)
                                   (vec2/scale 0.98))
                       vel-x (vec2/get-x new-vel)
                       new-facing (if (zero? vel-x) (reverse-dir facing) facing)
                       ]

                   (recur (inc n)
                          new-pos
                          new-vel
                          new-facing)))
               p)))))
      (remove! ekey))))

;; Lissajous figures
(defn xf [A a δ t]
  (* A (Math/sin (+ δ (* a t)))))

(defn yf [B b t]
  (* B (Math/sin (* b t))))

(defn e2-pos [start-pos n {:keys [A B a b δ]}]
  ;;(js/console.log A B a b d)
  (let [x (xf A a δ n)
        y (yf B b n)]
    (-> (vec2/vec2 x y)
        (vec2/add start-pos)
        )))

(defn facing [n {:keys [a]}]
  (pos? (Math/cos (+ (/ Math/PI 2) 0.5 (* a n)))))

(def figure-8 {:A 3
               :B 1
               :a 0.02
               :b 0.04
               :δ (/ Math/PI 2)})


(def opts
  {:enemy-2
   [
    ;; figure 8
    {:A 3
     :B 1
     :a 0.02
     :b 0.04
     :δ (/ Math/PI 2)
     :xoff 0
     :yoff 0}

    ;; ABC symbol
    {:A 4
     :B 1
     :a 0.01
     :b 0.03
     :δ (/ Math/PI 2)
     :xoff -0.5
     }

    ;; diagonal back and forth
    {:A 2
     :B -2
     :a 0.01
     :b 0.01
     :δ Math/PI
     :xoff -0.5
     }

    ;; circle
    {:A 1
     :B 1
     :a 0.05
     :b 0.05
     :δ (/ Math/PI 2)
     :xoff 1
     }

    ;; figure 8
    {:A 3
     :B 1
     :a 0.02
     :b 0.04
     :δ (/ Math/PI 2)
     :xoff 0
     :yoff 0.5}

    ;; circle
    {:A 2
     :B 2
     :a 0.05
     :b 0.05
     :δ (/ Math/PI 2)
     }


    ]})
(defn get-opts [enemy-type n]
  (let [res (get-in opts [enemy-type n])]
    (js/console.log res)
    res)
  )


(defmethod spawn :enemy-2 [container start-pos sprite &
                           [ind]]
  (let [start-game-num (:game-num @state/state)
        ekey (keyword (gensym))]
    (go
      (async/continue-while
       (= start-game-num (:game-num @state/state))
       (let [start-frame 0]
         (c/with-sprite container
           [enemy (s/make-sprite sprite :pos (vec2/scale start-pos 64))]
           (add! ekey {:pos start-pos} )
           (loop [n 0]
             (let [p (e2-pos (vec2/add start-pos
                                       (vec2/vec2 (get-in opts [:enemy-2 ind :xoff] 0)
                                                  (get-in opts [:enemy-2 ind :yoff] 0)
                                                  ))
                             n
                             (or (get-in opts [:enemy-2 ind]) figure-8))
                   ppos (vec2/scale p 64)
                   [x y] ppos
                   ]
               (swap! enemies assoc-in [ekey :pos] p)
               (s/set-pos! enemy x y))

             (s/set-scale! enemy
                           (if (facing n (or (get-in opts [:enemy-2 ind]) figure-8)) -1 1)
                           1)
             (<! (e/next-frame))
             (recur (inc n))))))
      (remove! ekey))))









(defmethod spawn :enemy-3 [container start-pos sprite & [ind]]
  (let [start-game-num (:game-num @state/state)
        ekey (keyword (gensym))]
    (go
      (async/continue-while
       (= start-game-num (:game-num @state/state))
       (let [start-frame 0]
         (c/with-sprite container
           [enemy (s/make-sprite sprite :pos (vec2/scale start-pos 64))]
           (add! ekey {:pos start-pos} )
           (loop [n 0
                  p start-pos
                  v (vec2/zero)
                  facing :left
                  ]
             (let [ppos (vec2/scale p 64)
                   [x y] ppos
                   px (vec2/get-x p)
                   py (vec2/get-y p)
                   pix (int px)
                   piy (int py)

                   square-on (tm/get-tile-at tm/tile-map pix piy)
                   square-below (tm/get-tile-at tm/tile-map pix (inc piy))
                   square-below-left (tm/get-tile-at tm/tile-map (dec pix) (inc piy))
                   square-below-right (tm/get-tile-at tm/tile-map (inc pix) (inc piy))
                   square-standing-on (tm/get-tile-at tm/tile-map pix
                                                      (int (+ 0.3 y)))
                   ]
               (swap! enemies assoc-in [ekey :pos] p)
               (s/set-pos! enemy x y)

               ;; stop processing when far away from player
               #_ (while (>
                          (vec2/magnitude-squared
                           (vec2/sub (:pos @state/state) p))
                          (* 50 50))
                    ;; far away from player. sleep for a bit (less CPU)
                    (<! (e/wait-time (int (+ 1000 (* 1000 (rand)))))))

               (s/set-scale! enemy (if (= :left facing) 1 -1) 1)
               (<! (e/next-frame))
               (if true ;;(< n 3000)
                 ;; still alive
                 (do
                   #_ (let [frame (int (/ n 60))
                            texture :rune-1]
                        (s/set-texture! sprite (t/get-texture texture)))

                   (let [platform-state
                         (-> platforms/platforms
                             (platforms/prepare-platforms n)
                             (platforms/filter-platforms (vec2/zero)))

                         new-pos
                         (constraints/constrain-pos
                          constraints/enemy-constrain
                          (-> platforms/platforms
                              (platforms/prepare-platforms (+ 1 start-frame n))
                              (platforms/filter-platforms p))
                          p (vec2/add (vec2/add p v) (vec2/vec2 (if (= :left facing) (- speed) speed) 0)))

                         new-vel (-> new-pos
                                     (vec2/sub p)

                                     (vec2/add consts/gravity)
                                     (vec2/scale 0.98))
                         vel-x (vec2/get-x new-vel)
                         new-facing (cond
                                      (and (= :left facing) (= :space square-below-left))
                                      :right

                                      (and (= :right facing) (= :space square-below-right))
                                      :left

                                      ;; hit wall
                                      (zero? vel-x)
                                      (reverse-dir facing)

                                      :default facing
                                      ;;(zero? vel-x) (reverse-dir facing) facing
                                      )
                         ]

                     (recur (inc n)
                            new-pos
                            new-vel
                            new-facing)))
                 p))))))
      (remove! ekey))))
