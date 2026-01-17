(ns com.biffweb.tasks
  "A collection of tasks used by Biff projects.")

(def tasks
  {"clean"            'com.biffweb.tasks.clean/clean
   "css"              'com.biffweb.tasks.css/css
   "deploy"           'com.biffweb.tasks.deploy/deploy
   "dev"              'com.biffweb.tasks.dev/dev
   "generate-config"  'com.biffweb.tasks.generate/generate-config
   "generate-secrets" 'com.biffweb.tasks.generate/generate-secrets
   "install-tailwind" 'com.biffweb.tasks.install-tailwind/install-tailwind
   "nrepl"            'com.biffweb.tasks.nrepl/nrepl
   "prod-dev"         'com.biffweb.tasks.prod-dev/prod-dev
   "logs"             'com.biffweb.tasks.ssh/logs
   "prod-repl"        'com.biffweb.tasks.ssh/prod-repl
   "restart"          'com.biffweb.tasks.ssh/restart
   "soft-deploy"      'com.biffweb.tasks.soft-deploy/soft-deploy
   "uberjar"          'com.biffweb.tasks.uberjar/uberjar})
