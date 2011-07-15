class PowertacFactoredCustomerGrailsPlugin {
    // the plugin version
    def version = "0.1"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "1.3.7 > *"
    // the other plugins this plugin depends on
    def dependsOn = ['powertacCommon':'0.10 > *',
                     'powertacServerInterface':'0.2 > *',
                     'powertacRandom':'0.2 > *']
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    // TODO Fill in these fields
    def author = "Prashant Reddy"
    def authorEmail = "ppr@cs.cmu.edu"
    def title = "Factored Customer Model"
    def description = '''\\
This plugin contains a "Factored Behaviors" statistical model of customer populations 
with consumption and/or production capacities. Behaviors such as tariff-utility, 
tariff-switching-inertia and elasticity-of-capacity can be specified directly using 
probability distributions, or alternately, they can be computed by the model using 
specified factors which underlie those behaviors.
'''

    // URL to the plugin's documentation
    def documentation = "https://github.com/powertac-plugins/powertac-factored-customer"

    def doWithWebDescriptor = { xml ->
        // TODO Implement additions to web.xml (optional), this event occurs before 
    }

    def doWithSpring = {
        // TODO Implement runtime spring config (optional)
    }

    def doWithDynamicMethods = { ctx ->
        // TODO Implement registering dynamic methods to classes (optional)
    }

    def doWithApplicationContext = { applicationContext ->
        // TODO Implement post initialization spring config (optional)
    }

    def onChange = { event ->
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    def onConfigChange = { event ->
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }
}
