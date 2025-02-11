package ru.vyarus.gradle.plugin.python.docker

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import ru.vyarus.gradle.plugin.python.AbstractKitTest
import ru.vyarus.gradle.plugin.python.util.CliUtils
import spock.lang.IgnoreIf

/**
 * @author Vyacheslav Rusakov
 * @since 21.09.2022
 */
// testcontainers doesn't work on windows server https://github.com/testcontainers/testcontainers-java/issues/2960
@IgnoreIf({ System.getProperty("os.name").toLowerCase().contains("windows") })
class DockerRunKitTest extends AbstractKitTest {

    def "Check simple execution"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {               
                docker.use = true
            }
            
            tasks.register('sample', PythonTask) {
                command = '-c print(\\'samplee\\')'
            }

        """

        when: "run task"
        debug()
        BuildResult result = run('sample')

        then: "task successful"
        result.task(':sample').outcome == TaskOutcome.SUCCESS
        result.output.contains('[docker] container')
        result.output.contains('samplee')
    }


    def "Check pip-powered execution"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {
                pip 'extract-msg:0.28.0'
                
                docker.use = true
            }
            
            tasks.register('sample', PythonTask) {
                command = '-c print(\\'samplee\\')'
            }

        """

        when: "run task"
        debug()
        BuildResult result = run('sample')

        then: "task successful"
        result.task(':sample').outcome == TaskOutcome.SUCCESS
        result.output.contains('[docker] container')
        result.output =~ /extract-msg\s+0.28.0/
        result.output.contains('samplee')
    }

    def "Check user-scoped execution"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {
                pip 'extract-msg:0.28.0'
                scope=USER
                
                docker.use = true
            }
            
            tasks.register('sample', PythonTask) {
                command = '-c print(\\'samplee\\')'
            }

        """

        when: "run task"
        debug()
        BuildResult result = run('sample')

        then: "task successful"
        result.task(':sample').outcome == TaskOutcome.SUCCESS
        result.output.contains('[docker] container')
        result.output =~ /extract-msg\s+0.28.0/
        result.output.contains('samplee')
    }


    def "Check in-container fail"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {               
                docker.use = true
            }
            
            tasks.register('sample', PythonTask) {
                command = '-c printTt(\\'samplee\\')'
            }

        """

        when: "run task"
        debug()
        BuildResult result = runFailed('sample')

        then: "task successful"
        result.task(':sample').outcome == TaskOutcome.FAILED
        result.output.contains('\'printTt\' is not defined')
    }

    def "Check port mappings"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {               
                docker.use = true
                docker.ports 5000, '5001:5020'
            }
            
            tasks.register('sample', PythonTask) {
                command = '-c print(\\'samplee\\')'
            }

        """

        when: "run task"
        debug()
        BuildResult result = run('sample')

        then: "task successful"
        result.task(':sample').outcome == TaskOutcome.SUCCESS
        result.output.contains('[docker] container')
        result.output.contains('samplee')
    }

    def "Check docker env cleanup"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {   
                docker.use = true             
                pip 'extract-msg:0.28.1'
            }            
        """

        when: "create env"
        BuildResult result = run('checkPython')

        then: "created"
        result.task(':checkPython').outcome == TaskOutcome.SUCCESS
        result.output.contains('-m virtualenv .gradle/python')
        file('.gradle/python').exists()

        when: "cleanup env"
        debug()
        result = run('cleanPython')

        then: "cleared"
        result.task(':cleanPython').outcome == TaskOutcome.SUCCESS
        !file('.gradle/python').exists()

        when: "create env again"
        result = run('checkPython')

        then: "created"
        result.task(':checkPython').outcome == TaskOutcome.SUCCESS
        result.output.contains('-m virtualenv .gradle/python')
        file('.gradle/python').exists()
    }

    def "Check manual own call"() {
        setup:
        build """
            plugins {
                id 'base'
                id 'ru.vyarus.use-python'
            }

            python {               
                docker.use = true
            }
            
            tasks.register('sample', PythonTask) {
                command = '-c "with open(\\'build/temp.txt\\', \\'w+\\') as f: pass"'
                doLast {
                    dockerChown 'build/temp.txt'
                }
            }

        """
        file('build').mkdir()

        when: "run task"
        debug()
        BuildResult result = run('sample')

        then: "task successful"
        result.task(':sample').outcome == TaskOutcome.SUCCESS
        result.output.contains('[docker] container')
        if (CliUtils.linuxHost) {
            result.output.contains('chown')
        } else {
            !result.output.contains('chown')
        }
        file('build/temp.txt').exists()

        when: "do clean"
        result = run('clean')

        then: "clean ok"
        result.task(':clean').outcome == TaskOutcome.SUCCESS
        !file('build').exists()
    }

    def "Check docker command execution"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

            python {               
                docker.use = true
            }
            
            tasks.register('sample', PythonTask) {
                doFirst {
                    dockerExec 'ls -l /usr/src/'
                }
                command = '-c print(\\'samplee\\')'
            }

        """

        when: "run task"
        debug()
        BuildResult result = run('sample')

        then: "task successful"
        result.task(':sample').outcome == TaskOutcome.SUCCESS
        result.output.contains('[docker] container')
        result.output.contains('samplee')
        result.output.contains('ls -l')
    }

}
