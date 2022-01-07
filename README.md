# plug-sso

Pluggable auth handling reducing boilerplate.

## Status

* Experimental proof-of-concept.
* Breakage to be expected while iterating towards a working design
  * Version will stay at `0.1.0-SNAPSHOT` during this time.
* Several planned security measures missing.
* Aims to provide reasonable security for internal tools, not internet facing ones.
* Quite likely to be split into two parts later. Refer to "Design" section for details on the parts.

# Development

First time and after update of package.json

    npm install

Backend / web server (script reads .env file)

    lein run

Frontend

    shadow-cljs watch app

URLs

* [http://localhost:3300](http://localhost:3300) - Admin UI
* Service API runs on port 3300 (same as UI)

Note on ports:

* By default, a Luminus based project uses port 3000. As this project includes a service endpoint that is designed to be
  used by another app, port is changed to 3300 to allow it to run locally alongside another Luminus based app that
  accesses the service endpoint during development.
* The same "repeat first digit" approach is for REPL ports as well.

REPLs:

* Backend nREPL: 7700
* Frontend nREPL: 7702

# Usage

## Important conventions

* API routes contains `/api` as part of URI.
  * This is used to differentiate page requests (redirect to `/login`) from AJAX (`401` API responses)

## Environment

* ...

## How to hook it up

* Using Luminus project `"foo"` as example.
* All namespaces below uses require `[plug-sso.lib.core :as sso-lib]`

### Add auth routes (providing /login, /logout, ...)

```clojure
(ns foo.handler)

;; Locate the following and add the extra routes :
(mount/defstate app-routes
  :start
  (ring/ring-handler
    (ring/router
      [(home-routes)
       (sso-lib/auth-routes {:app            "foo"          ;; <<<<< ADD THIS 
                             :reset-capable? true           ;; true if you have email support (refer to ENVIRONMENT in Readme for details)
                             :sso-host       "localhost"    ;; Host where SSO service is running
                             :sso-port       3300})],,,)))  ;; Port where SSO service is running
``` 

### Middleware for auth handling

```clojure
(ns foo.middleware)

;; NOTE: Slightly modified "wrap-base" to void issue with prone.middleware/wrap-exceptions during DEV
(defn wrap-base [handler]
  (let [defaults-middleware (:middleware defaults)]
    (-> handler
        sso-lib/wrap-auth                      ;; <<<<< ADD THIS (before Luminus defaults middleware)
        defaults-middleware,,,)))
```

### Middleware to protect routes

```clojure
(ns foo.routes.home)

;; Add middleware to protect routes
(defn home-routes
  []
  [""
   {:middleware [sso-lib/wrap-protected,,,]}],,,) ;; <<<<< ADD THIS
```

### Config

Main config relies on a file called `ansible_config.yml`
This file is generated from Ansible, written to a secure place and symlinked into project (dev).
`ansible_config.yml` is present in `.gitignore`. In prod it will reside next to server.jar

#### Email support requires the following config keys:

```shell
:smtp-host
:smtp-port
:smtp-user
:smtp-pass
```

# Design

Two main parts:

* Backend
  * Admin GUI
  * DB ([Datalevin](https://github.com/juji-io/datalevin))
  * API for lib
  * ...
* Library
  * Auth routes
  * HTML templates
  * Contacts backend API
  * ...

# Resources

* [Datalevin](https://github.com/juji-io/datalevin) -- Database
* [Reagent](http://reagent-project.github.io) -- React interface
* [re-frame](http://day8.github.io/re-frame/) -- Reactive state management for Reagent
* [Bulma](https://bulma.io/documentation/) -- Styling
* [Postal](https://github.com/drewr/postal) -- SMTP
* [Bouncer](https://github.com/theleoborges/bouncer) -- Validation
* Buddy -- Security
  * [buddy-auth](https://github.com/funcool/buddy-auth)
  * [buddy-sign](https://github.com/funcool/buddy-sign)
  * [buddy-core](https://github.com/funcool/buddy-core)
  * [buddy-hashers](https://github.com/funcool/buddy-hashers)
  * examples:
    * [authexample](https://github.com/funcool/buddy-auth/blob/master/examples/session/src/authexample/web.clj)

# Background

Generated using Luminus version "4.15" with:

`lein new luminus plug-sso +auth +shadow-cljs +re-frame`

## License

Copyright © 2021 Morten Kristoffersen

This program and the accompanying materials are made available under the terms of the Eclipse Public License 2.0 which
is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary Licenses when the conditions for such
availability set forth in the Eclipse Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your option) any later version, with the GNU
Classpath Exception which is available at https://www.gnu.org/software/classpath/license.html.
