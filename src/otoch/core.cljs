(ns otoch.core
  (:require [infinitelives.pixi.resources :as r]
            [infinitelives.pixi.texture :as t]
            [infinitelives.pixi.canvas :as c]
            [infinitelives.pixi.sprite :as s]
            [infinitelives.pixi.pixelfont :as pf]
            [infinitelives.utils.events :as e]
            [infinitelives.utils.gamepad :as gp]
            [infinitelives.utils.vec2 :as vec2]
            [infinitelives.utils.console :refer [log]]
            [cljs.core.match :refer-macros [match]]
            [clojure.string :as string]
            [otoch.line :as line]
            [otoch.map :as tm]
            [otoch.consts :as consts]
            [otoch.state :as state]
            [otoch.constraints :as constraints]
            [otoch.platforms :as platforms]
            [otoch.enemy :as enemy]
            [cljs.core.async :refer [chan close! >! <! timeout]]
)
  (:require-macros [cljs.core.async.macros :refer [go]]
                   ;;[infinitelives.pixi.macros :as m]
                   [infinitelives.pixi.pixelfont :as pf]
))

(enable-console-print!)

(println "This text is printed from src/otoch/core.cljs. Go ahead and edit it and see reloading in action.")

;; define your app data so that it doesn't get over-written on reload

(defonce app-state (atom {:text "Hello world!"}))


(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)


(defonce bg-colour 0xb8f3e8)

(def sprite-sheet-layout
  {
   :rune-1 {:pos [(* 3 64) (* 9 64)] :size [64 64]}

   :megalith {:size [64 64] :pos [(* 4 64) (* 2 64)]}

   :enemy {:size [64 64] :pos [(* 64 3) (* 64 7)]}

   :explosion-1 {:size [16 16] :pos [48 48]}
   :explosion-2 {:size [16 16] :pos [64 48]}
   :explosion-3 {:size [16 16] :pos [80 48]}
   :explosion-4 {:size [16 16] :pos [96 48]}
   :explosion-5 {:size [16 16] :pos [112 48]}
   :explosion-6 {:size [16 16] :pos [128 48]}

   :gold {:size [16 16] :pos [0 32]}
   })

(defn make-foreground-map [bg-map-lines]
  (tm/mapv-mapv
   bg-map-lines
   (fn [c]
     (cond
       (#{:clover :grass-fg-1 :grass-fg-2 :grass-fg-3 :cactus-fg :reeds-fg} c)
       c

       ;;(= c :ladder-top) :ladder-top-fg
       :default :space))))


(defonce canvas
  (c/init {:layers [:bg :tilemap :stats :title :ui]
           :background bg-colour
           :expand true
           :origins {:stats :bottom-left
                     :title :top}
           :translate {:stats [40 -40]
                       :title [0 90]}}))

(s/set-default-scale! 1)

(defn set-player [player x y px py]
  (s/set-pos!
   player
   (+ x (* 16 4 px)) (+ y (* 16 4 py))))


(defn hollow
  "if vector is less than a certain size, make it zero"
  [v lim]
  (if (< (vec2/magnitude-squared v) lim)
    (vec2/zero)
    v))

(defn jump-button-pressed? []
  (or
   (e/is-pressed? :z)
   (e/is-pressed? :space)
   (gp/button-pressed? 0 :x)))


;; work out if we are standing on a platform, and if
;; so, which one?  to do this, we simulate the
;; player falling every so slightly by a small
;; amount and by a larger amount. we then constrain
;; both these movements by the platform in
;; question. If the y value is the same between the
;; two end points, then we are standing on the
;; platform (even if platform is moving!)
(defn which-platform? [old-pos plats]
  (let [start old-pos
        num (count plats)
        end1 (vec2/add old-pos (vec2/vec2 0 0.1))
        end2 (vec2/add old-pos (vec2/vec2 0 0.3))]
    (loop [n 0]
      (let [{:keys [passable? platform-pos]} (nth plats n)]
        (if (= (vec2/get-y (constraints/platform-constrain passable? platform-pos start end1))
               (vec2/get-y (constraints/platform-constrain passable? platform-pos start end2)))
          n
          (when (< n (dec num))
            (recur (inc n))))))))

(defn make-text-display
  "create an updating number display with icon. Used for gold/dynamite etc.
  icon and text appeads at `y` with font `font` displaying string `s`.
  returns a channel. When you push a value down the channel, the
  display will update to that pushed value. When you close! the channel,
  the text disappears.
  "
  [icon y font s]
  (let [c (chan)]
    (go
      (c/with-sprite :stats
        [icon (s/make-sprite icon :scale 4 :y (+ y -5))
         text (pf/make-text font (str s) :scale 4 :xhandle 0 :x 50 :y y)]
        (loop [s (<! c)]
          (pf/change-text! text font (str s))
          (s/update-handle! text 0 0.5)
          (when-let [res (<! c)]
            (recur res)))))
    c))

(def gravity (vec2/vec2 0 0.01))

(defn make-dynamite [container pos vel start-frame]
  (go
    (c/with-sprite container
      [sprite (s/make-sprite :rune-1 :scale 0.5 :yhandle 0 :x (vec2/get-x pos) :y (vec2/get-y pos))]
      (let [final-pos (loop [n 0
                             p pos
                             v vel]
                        (s/set-pos! sprite (vec2/scale p 64))
                        (<! (e/next-frame))
                        (if (< n 300)
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
                                   constraints/dynamite-constrain
                                   (-> platforms/platforms
                                       (platforms/prepare-platforms (+ 1 start-frame n))
                                       (platforms/filter-platforms p))
                                   p (vec2/add p v))
                                  new-vel (-> new-pos
                                              (vec2/sub p)
                                              (vec2/add gravity)
                                              (vec2/scale 0.98))]
                              (recur (inc n)
                                     new-pos
                                     new-vel)))

                          ;; rune over. return final pos
                          p))]

        ;; megalith rise
        (s/set-texture! sprite :megalith)
        (s/set-scale! sprite 1)
        (s/set-anchor-y! sprite 0.5)

        (let [[final-pos final-vel] (loop [n 300
                                           p final-pos
                                           v (vec2/zero)
                                           rise 1
                                           ]
                                      (let [new-pos (vec2/scale (vec2/add p (vec2/vec2 0 rise)) 64)
                                            [x y] new-pos]
                                        (s/set-pos! sprite (int x) (int y) ))
                                      (<! (e/next-frame))

                                      (if (< n (+ 300 600))
                                        (let [platform-state
                                                 (-> platforms/platforms
                                                     (platforms/prepare-platforms n)
                                                     (platforms/filter-platforms (vec2/zero)))
                                                 new-pos
                                                 (constraints/constrain-pos
                                                  constraints/dynamite-constrain
                                                  (-> platforms/platforms
                                                      (platforms/prepare-platforms (+ 1 start-frame n))
                                                      (platforms/filter-platforms p))
                                                  p (vec2/add p v))
                                                 new-vel (-> new-pos
                                                             (vec2/sub p)
                                                             (vec2/add gravity)
                                                             (vec2/scale 0.98))]
                                             (recur (inc n)
                                                    new-pos
                                                    new-vel
                                                    (- rise (/ 1 600))))

                                        [p v]))]

          ;; megalith runs forever
          (loop [
                 n (+ 600 300)
                 p final-pos
                 v final-vel
                 ]
            (s/set-pos! sprite (vec2/scale (vec2/add p (vec2/vec2 0 0)) 64))
            (<! (e/next-frame))

            (let [platform-state
                  (-> platforms/platforms
                      (platforms/prepare-platforms n)
                      (platforms/filter-platforms (vec2/zero)))
                  new-pos
                  (constraints/constrain-pos
                   constraints/dynamite-constrain
                   (-> platforms/platforms
                       (platforms/prepare-platforms (+ 1 start-frame n))
                       (platforms/filter-platforms p))
                   p (vec2/add p v))
                  new-vel (-> new-pos
                              (vec2/sub p)
                              (vec2/add gravity)
                              (vec2/scale 0.98))]
              (recur (inc n)
                     new-pos
                     new-vel
                     ))))

        )

      ;; explode
      #_ (loop [[f & r] [:explosion-1 :explosion-2
                         :explosion-3 :explosion-4
                         :explosion-5 :explosion-6]]
           (s/set-texture! sprite (t/get-texture f))
           (<! (e/next-frame))
           (<! (e/next-frame))
           (when r
             (recur r))))))


(defonce main
  (go
    ;; load image tilesets
    (<! (r/load-resources canvas :ui ["img/tiles.png"
                                      "img/fonts.png"
                                      "img/title.png"]))

    ;; load textures
    (t/load-sprite-sheet! (r/get-texture :tiles) sprite-sheet-layout)
    (t/load-sprite-sheet! (r/get-texture :title)
                          {:title {:pos [0 0]
                                   :size [652 214]}})

    ;; make a number font
    (pf/pixel-font :numbers "img/fonts.png" [10 10] [248 70]
                   :chars "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789#`'.,"
                   :space 5)

    ;; make a small pixelly font
    (pf/pixel-font :pixel "img/fonts.png" [10 10] [248 70]
                   :chars "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789#`'.,"
                   :space 3)

    #_ (go
         (c/with-sprite :title
           [title (pf/make-text :pixel "OTOCH"
                                :scale 4 :xhandle 0.5
                                :tint 0xff8000)

            ]
           (loop []
             (<! (e/next-frame))
             (recur))))

    (go
      (c/with-sprite :title
        [title (s/make-sprite :title
                              :scale 1
                              :xhandle 0.5
                              :yhandle 0.5
                              :alpha 1.0)]
        (loop [n 0]
          (let [frac (/ n 20)]
            (s/set-pos! title 0 (- 250 (* frac frac 100)))
            (s/set-scale! title (/ 2 (inc (* frac frac))))
            (<! (e/next-frame))
            (when (< n 150)
              (recur (+ n 0.1))))))
      )

    ;; make the tile texture lookup
    (let [tile-set (tm/make-tile-set :tiles)
          stand (t/sub-texture (r/get-texture :tiles :nearest) [0 (* 4 96)] [64 64])
          walk (t/sub-texture (r/get-texture :tiles :nearest) [(* 4 16) (* 4 96)] [64 64])
          tilemap-order-lookup (tm/make-tiles-struct tile-set tm/tile-map)

          dynamite (make-text-display :rune-1 0 :numbers 10)
          gold (make-text-display :gold -64 :numbers 0)
          ]

      #_(c/with-sprite canvas :tilemap
          [tilemap  (s/make-container
                     :children (tm/make-tiles tile-set tile-map)
                     :xhandle 0 :yhandle 0
                     :scale 1
                     :particle true
                     :particle-opts #{:alpha})]


          )

      ;; create sprite and tile map batches
      (c/with-sprite canvas :tilemap
        [ ;; background (js/PIXI.TilingSprite.
         ;;             (t/sub-texture
         ;;              (r/get-texture :tiles :nearest)
         ;;              [0 48] [32 32])
         ;;             1000 1000)

         dynamites (s/make-container :scale 1 :particle false)

         tilemap (s/make-container
                  :children (tm/make-tiles tile-set tm/tile-map)
                  :xhandle 0 :yhandle 0
                  :scale 1
                  ;;:particle true
                  ;;:particle-opts #{:alpha}
                  )
         platform (s/make-container
                   :children (tm/make-tiles tile-set platforms/platform-map)
                   ;; :xhandle 0 :yhandle 0
                   :scale 1
                   ;;:particle true
                   )
         ;; platform2 (s/make-container
         ;;            :children (tm/make-tiles tile-set platform2-map)
         ;;            :xhandle 0 :yhandle 0
         ;;            :scale 1
         ;;            :particle true)
         ;; platform3 (s/make-container
         ;;            :children (tm/make-tiles tile-set platform2-map)
         ;;            :xhandle 0 :yhandle 0
         ;;            :scale 1
         ;;            :particle true)
         player (s/make-sprite stand :scale 1)
         foreground (s/make-container
                     :children (tm/make-tiles tile-set (into [] (make-foreground-map tm/tile-map)))
                     ;;:xhandle 0 :yhandle 0
                     :scale 1
                     ;;:particle true
                     )

         enemies (s/make-container)


         ]

        (enemy/spawn enemies (vec2/vec2 20 5))

        ;;(s/set-scale! background 1)
        (loop [
               state :walking
               fnum 0
               old-vel (vec2/vec2 0 0)
               ppos (vec2/vec2 1.5 4.5)
               jump-pressed 0
               gold-num 0
               dynamite-num 10
               last-x-pressed? (e/is-pressed? :x)
               facing :left
               ]

          (let [
                old-pos ppos
                pos (-> ppos
                        (vec2/scale (* -2 32)))
                x (vec2/get-x pos) ;; (+ -2000 (* 1000 (Math/sin theta)))
                y (vec2/get-y pos) ;; (+ -1000 (* 500 (Math/cos theta)))

                px (vec2/get-x ppos)
                py (vec2/get-y ppos)
                pix (int px)
                piy (int py)
                dx (- px pix)
                dy (- py piy)

                joy (vec2/vec2 (or (gp/axis 0)
                                   (cond (e/is-pressed? :left) -1
                                         (e/is-pressed? :right) 1
                                         :default 0) )
                               (or (gp/axis 1)
                                   (cond (e/is-pressed? :up) -1
                                         (e/is-pressed? :down) 1
                                         :default 0)
                                   ))

                platforms-this-frame (platforms/prepare-platforms platforms/platforms fnum)

                ;; platform subset for player
                filtered-platforms (platforms/filter-platforms
                                    platforms-this-frame old-pos)
                ]

            (s/set-texture! player
                            (if (> (Math/abs (vec2/get-x joy)) 0.02)
                              (if (zero? (mod (int (/ fnum 10)) 2)) stand walk)
                              stand))
            (set-player player x y px py)
            (s/set-pos! dynamites x y)

            ;;(js/console.log "===>" (str platforms-this-frame))

            ;; set tilemap positions
            (doall
             (map
              (fn [{:keys [name old-platform-pos]} obj]
                (let [pos (-> old-platform-pos
                              (vec2/scale (* 4 16))
                              (vec2/add (vec2/vec2 x y)))]
                  (s/set-pos! obj pos)))
              platforms-this-frame
              [tilemap platform ;;platform2 platform3
               ]))


            (s/set-pos! foreground x y)

            #_ (s/set-pos! background
                           (+ -2000 (mod (int (* x 0.90)) (* 4 32)))
                           (+ -2000 (mod (int (* y 0.90)) ( * 4 32))))

            ;; save level pos so other go blocks can access
            ;; (swap! state/state assoc :pos (vec2/vec2 x y))

            (s/set-pos! enemies x y)

            (<! (e/next-frame))
                                        ;(log dy minus-v-edge)
            (let [
                  square-on (tm/get-tile-at tm/tile-map pix piy)
                  square-below (tm/get-tile-at tm/tile-map pix (inc piy))
                  square-standing-on (tm/get-tile-at tm/tile-map pix
                                                     (int (+ 0.3 py)))

                  on-ladder-transition? (or (= square-on :ladder)
                                            (= square-on :ladder-top)
                                            (= square-below :ladder-top))

                  on-ladder? (#{:ladder :ladder-top} square-standing-on)

                  on-gold? (= :gold square-standing-on)
                  new-gold (or
                            (when on-gold?
                              (when (tilemap-order-lookup [pix piy])
                                (let [child
                                      (.getChildAt tilemap
                                                   (tilemap-order-lookup [pix piy]))]
                                  (when (= 1 (.-alpha child))
                                    (s/set-alpha! child 0)
                                    (>! gold (inc gold-num))
                                    (inc gold-num)))))
                            gold-num)

                  new-dynamite (if (e/is-pressed? :r) (inc dynamite-num) dynamite-num)
                  _ (when (not= dynamite-num new-dynamite)
                      (>! dynamite new-dynamite))

                  ladder-up? (#{:ladder :ladder-top} square-standing-on)
                  ladder-down? (#{:ladder :ladder-top} square-below)

                  plat (which-platform? old-pos filtered-platforms)

                  ;;_ (js/console.log "PLAT:" plat)

                  ;; move oldpos by platform movement
                  old-pos (if plat
                            (-> filtered-platforms
                                (nth plat)
                                :platform-delta
                                (vec2/add old-pos))
                            old-pos)

                  ;; simulate a little vertical move down to see if we are
                  ;; standing on solid ground (or a platform)
                  fallen-pos
                  (constraints/constrain-pos constraints/platform-constrain filtered-platforms
                                 old-pos (vec2/add old-pos (vec2/vec2 0 0.1)))

                  ;; TODO: this is dodgy. We need to test with each platform.
                  ;; if we are standing on a platform, we are "bound to it"
                  ;; and we move where it does!
                  standing-on-ground? (> 0.06 (Math/abs (- (vec2/get-y fallen-pos) (vec2/get-y old-pos))))

                  jump-pressed (cond
                                 (and (jump-button-pressed?) (zero? jump-pressed) standing-on-ground?) ;; cant jump off ladder! if you can, problem... when jumping off lader, state stays climbing causing no accel for the jump
                                 (inc jump-pressed)

                                 (and (jump-button-pressed?) (pos? jump-pressed))
                                 (inc jump-pressed)

                                 :default
                                 0)

                  jump-force (if (and (<= 1 jump-pressed consts/jump-frames)
                                      (jump-button-pressed?))
                               (vec2/vec2 0 (- (if (= 1 jump-pressed)
                                                 consts/jump-accel-1
                                                 consts/jump-accel-2+)))
                               (vec2/zero))

                  joy-dy (-> joy
                             (hollow 0.2)
                             (vec2/get-y))

                  joy-dy (cond
                           (and (not ladder-up?) (neg? joy-dy))
                           0

                           (and (not ladder-down?) (not ladder-up?) (pos? joy-dy))
                           0

                           :default joy-dy)

                  joy-dx (-> joy
                             (hollow 0.2)
                             (vec2/scale 0.005)
                             (vec2/get-x))

                  state-ladder? (and on-ladder-transition?
                                     (not (zero? joy-dy)))

                  joy-acc (if (= :climbing state)
                            (vec2/vec2 joy-dx (* 0.1 joy-dy))
                            (vec2/vec2 joy-dx 0))


                  player-vel-x (Math/abs (vec2/get-x old-vel))

                  ;; when moving quickly left and right, and the
                  ;; joystick is centered or reversed, this breaking
                  ;; horitontal force is applied
                  player-brake
                  (match [(Math/sign (vec2/get-x old-vel))
                          (Math/sign (vec2/get-x (hollow joy 0.5)))]
                         [1 0] (vec2/vec2 -1 0)
                         [1 -1] (vec2/vec2 -1 0)
                         [-1 0] (vec2/vec2 1 0)
                         [-1 1] (vec2/vec2 1 0)
                         [_ _] (vec2/zero))


                  next-state (if state-ladder?
                               :climbing
                               (if on-ladder? state :walking))

                  passable-fn (if (= :walking next-state)
                                platforms/walkable?
                                platforms/passable?)

                  new-vel (-> old-vel
                              ;; zero any y vel in the last frames vel if we are climbing
                              (vec2/set-y (if (= state :climbing) 0 (vec2/get-y old-vel)))
                              ;; no gravity on you when you are on the stairs
                              (vec2/add (if (= state :climbing) (vec2/zero) gravity))

                              (vec2/add jump-force)
                              (vec2/add joy-acc)
                              (vec2/add (vec2/scale player-brake (/ player-vel-x 3)))
                              (vec2/scale 0.98)
                              )

                  new-pos (-> old-pos
                              (vec2/add new-vel))

                  con-pos
                  (constraints/constrain-pos constraints/platform-constrain
                                 (assoc-in filtered-platforms [0 :passable?] passable-fn)
                                 old-pos new-pos)

                  old-vel (if (= :walking state) (vec2/sub con-pos old-pos)
                              (-> (vec2/sub con-pos old-pos)
                                  (vec2/set-y 0)))

                  new-dynamite
                  (if (and (pos? new-dynamite) (not last-x-pressed?) (e/is-pressed? :x))
                       (do (make-dynamite
                            dynamites ppos old-vel fnum)
                           (>! dynamite (dec new-dynamite))
                           (dec new-dynamite))
                       new-dynamite)]
              (case facing
                :left (s/set-scale! player -1 1)
                :right (s/set-scale! player 1 1)
                )

              (recur
               next-state
               (inc fnum)
               old-vel
               con-pos
               jump-pressed
               new-gold
               new-dynamite
               (e/is-pressed? :x)
               (case (Math/sign joy-dx)
                 -1 :left
                 0 facing
                 1 :right
                 )
               ))))))))
