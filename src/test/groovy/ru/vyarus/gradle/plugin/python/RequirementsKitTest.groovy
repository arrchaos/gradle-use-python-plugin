package ru.vyarus.gradle.plugin.python

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

/**
 * @author Vyacheslav Rusakov
 * @since 26.08.2022
 */
class RequirementsKitTest extends AbstractKitTest {

    def "Check strict requirements support"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }

        """
        file('requirements.txt') << """
# comment
extract-msg == 0.34.3

# vcs syntax (note, it's not valid syntax for pip due to version in egg part!) 
git+https://github.com/ictxiangxin/boson/@ea7d9113f71a7eb79083208d4f3bbb74feeb149f#egg=boson-1.4

# features syntax
requests[socks,security] == 2.28.1
"""

        when: "run task"
        BuildResult result = run('pipInstall')

        then: "task successful"
        result.task(':pipInstall').outcome == TaskOutcome.SUCCESS
        result.output.contains('-m virtualenv .gradle/python'.replace('/', File.separator))
        result.output =~ /extract-msg\s+0.34.3/
        result.output =~ /boson\s+1.4/
        result.output =~ /requests\s+2.28.1/
    }

    def "Check non-strict requirements support"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }
            
            python.requirements.strict = false

        """
        file('requirements.txt') << """
# comment
extract-msg == 0.34.3

# vcs syntax (without version part!)
git+https://github.com/ictxiangxin/boson/@ea7d9113f71a7eb79083208d4f3bbb74feeb149f#egg=boson

# features syntax
requests[socks,security] == 2.28.1
"""

        when: "run task"
        BuildResult result = run('pipInstall')

        then: "task successful"
        result.task(':pipInstall').outcome == TaskOutcome.SUCCESS
        result.output.contains('-m virtualenv .gradle/python'.replace('/', File.separator))
        result.output.contains('-m pip install -r requirements.txt')
        result.output =~ /extract-msg\s+0.34.3/
        result.output =~ /boson\s+1.4/
        result.output =~ /requests\s+2.28.1/
    }

    def "Check strict requirements ignore"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }
            
            python.requirements.use = false

        """
        file('requirements.txt') << """
# comment
extract-msg == 0.34.3

# vcs syntax
git+https://github.com/ictxiangxin/boson/@ea7d9113f71a7eb79083208d4f3bbb74feeb149f#egg=boson-1.4

# features syntax
requests[socks,security] == 2.28.1
"""

        when: "run task"
        BuildResult result = run('pipInstall')

        then: "task successful"
        result.task(':pipInstall').outcome == TaskOutcome.SKIPPED
    }

    def "Check non-strict requirements ignore"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }
            
            python.requirements {
                use = false
                strict = false
            }

        """
        file('requirements.txt') << """
# comment
extract-msg == 0.34.3

# vcs syntax (without version part!)
git+https://github.com/ictxiangxin/boson/@ea7d9113f71a7eb79083208d4f3bbb74feeb149f#egg=boson

# features syntax
requests[socks,security] == 2.28.1
"""

        when: "run task"
        BuildResult result = run('pipInstall')

        then: "task successful"
        result.task(':pipInstall').outcome == TaskOutcome.SKIPPED
    }

    def "Check strict requirements mix with direct modules"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }
            
            python.pip 'requests[socks,security]:2.28.1' 

        """
        file('requirements.txt') << """
# comment
extract-msg == 0.34.3
"""

        when: "run task"
        BuildResult result = run('pipInstall')

        then: "task successful"
        result.task(':pipInstall').outcome == TaskOutcome.SUCCESS
        result.output.contains('-m virtualenv .gradle/python'.replace('/', File.separator))
        result.output =~ /extract-msg\s+0.34.3/
        result.output =~ /requests\s+2.28.1/
    }

    def "Check strict requirements override with direct module"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }
            
            python.pip 'extract-msg:0.34.3' 

        """
        file('requirements.txt') << """
# comment
extract-msg==0.28.0
"""

        when: "run task"
        BuildResult result = run('pipInstall')

        then: "task successful"
        result.task(':pipInstall').outcome == TaskOutcome.SUCCESS
        result.output.contains('-m virtualenv .gradle/python'.replace('/', File.separator))
        !result.output.contains('0.28.0')
        result.output =~ /extract-msg\s+0.34.3/
    }


    def "Check non-strict requirements mix with direct modules"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }
            
            python {
                pip 'requests[socks,security]:2.28.1' 
                requirements.strict = false
            }

        """
        file('requirements.txt') << """
# comment
extract-msg == 0.34.3
"""

        when: "run task"
        BuildResult result = run('pipInstall')

        then: "task successful"
        result.task(':pipInstall').outcome == TaskOutcome.SUCCESS
        result.output.contains('-m virtualenv .gradle/python'.replace('/', File.separator))
        result.output.contains('-m pip install -r requirements.txt')
        result.output =~ /extract-msg\s+0.34.3/
        result.output =~ /requests\s+2.28.1/
    }

    def "Check requirements file rename"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }
            
            python.requirements {
                strict = false
                file = 'reqs.txt'
            }

        """
        file('reqs.txt') << """
# comment
extract-msg == 0.34.3
"""

        when: "run task"
        BuildResult result = run('pipInstall')

        then: "task successful"
        result.task(':pipInstall').outcome == TaskOutcome.SUCCESS
        result.output.contains('-m virtualenv .gradle/python'.replace('/', File.separator))
        result.output.contains('-m pip install -r reqs.txt')
        result.output =~ /extract-msg\s+0.34.3/
    }

    def "Check non-strict mode up to date check"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }
            
            python.requirements {
                strict = false
                file = 'requirements.txt'
            }

        """
        file('requirements.txt') << """
# comment
extract-msg == 0.34.3
"""

        when: "run task"
        BuildResult result = run('pipInstall')

        then: "task successful"
        result.task(':pipInstall').outcome == TaskOutcome.SUCCESS

        when: "run again"
        result = run('pipInstall')

        then: "task successful"
        result.task(':pipInstall').outcome == TaskOutcome.UP_TO_DATE
    }

    def "Check non-strict mode up to date check for changed file"() {
        setup:
        build """
            plugins {
                id 'ru.vyarus.use-python'
            }
            
            python.requirements {
                strict = false
                file = 'requirements.txt'
            }

        """
        file('requirements.txt') << """
# comment
extract-msg == 0.34.3
"""

        when: "run task"
        BuildResult result = run('pipInstall')

        then: "task successful"
        result.task(':pipInstall').outcome == TaskOutcome.SUCCESS
        result.output =~ /extract-msg\s+0.34.3/

        when: "run again with changed requirements"
        file('requirements.txt').delete()
        file('requirements.txt') << """
# comment
extract-msg == 0.28.0
"""
        result = run('pipInstall')

        then: "task successful"
        result.task(':pipInstall').outcome == TaskOutcome.SUCCESS
        result.output =~ /extract-msg\s+0.28.0/
    }
}
