package weaver.plugin.transform

import javassist.CtClass
import org.gradle.api.logging.Logger
import weaver.common.WeaveEnvironment
import weaver.plugin.model.TransformBundle
import weaver.plugin.processor.ProcessorInstantiator
import weaver.plugin.processor.WeaveEnvironmentImp
import weaver.plugin.util.Disposable

/**
 * <code>TransformerDelegate</code> is a delegate for all transformer tasks and it's responsible to instantiate and call processors.
 *
 * @author Saeed Masoumi (saeed@6thsolution.com)
 */
class TransformerDelegate implements Disposable {

    TransformBundle bundle
    ProcessorInstantiator processorInstantiator
    Logger logger

    TransformerDelegate(TransformBundle bundle) {
        this.bundle = bundle
        this.logger = bundle.project.logger
        processorInstantiator = new ProcessorInstantiator(bundle)
    }

    void execute() {
        def processors = processorInstantiator.instantiate()
        if (!processors)
            return
        def pool = bundle.classPool
        WeaveEnvironment env = new WeaveEnvironmentImp(logger, bundle.classPool, bundle.outputDir)
        processors.each {
            it.init(env)
        }
        def classFiles = bundle.classFiles
        Set<CtClass> classesSet = new HashSet<>();
        classFiles.each {
            classesSet.add(pool.get(it))
        }
        processors.each {
            try {
                it.transform(classesSet)
            } catch (all) {
                log "Skip processor with class name [${it.class.canonicalName}] \n" +
                        "An error occurred during transformation: \n " +
                        "$all.message "
            }
        }
    }

    @Override
    void dispose() {
        processorInstantiator.dispose()
    }

    void log(String message) {
        logger.info(message)
    }
}
