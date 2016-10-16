/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.omg.dmn.tck.runner.junit4;

import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.InitializationError;
import org.omg.dmn.tck.marshaller.TckMarshallingHelper;
import org.omg.dmn.tck.marshaller._20160719.TestCases;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DmnTckRunner
        extends ParentRunner<TestCases.TestCase> {

    private static final Logger logger = LoggerFactory.getLogger( DmnTckRunner.class );

    // The description of the test suite
    private final ConcurrentMap<TestCases.TestCase, Description> children = new ConcurrentHashMap<TestCases.TestCase, Description>();
    private final DmnTckVendorTestSuite vendorSuite;
    private       Description           descr;
    private       TestSuiteContext      context;
    private       TestCases             tcd;
    private       URI                   modelURI;

    public DmnTckRunner(DmnTckVendorTestSuite vendorSuite, File tcfile)
            throws InitializationError {
        super( vendorSuite.getClass() );
        this.vendorSuite = vendorSuite;
        try {
            tcd = TckMarshallingHelper.load( new FileInputStream( tcfile ) );
            String parent = tcfile.getParent();
            modelURI = new File( parent != null ? parent + "/" + tcd.getModelName() : tcd.getModelName() ).toURI();
            String tcdname = tcfile.getName();
            tcdname = tcdname.substring( 0, tcdname.lastIndexOf( '.' ) ).replaceAll( "\\.", "/" );
            this.descr = Description.createSuiteDescription( tcdname );
            for ( TestCases.TestCase test : tcd.getTestCase() ) {
                Description testDescr = Description.createTestDescription( tcdname, test.getId() );
                children.put( test, testDescr );
                this.descr.addChild( testDescr );
            }
        } catch ( FileNotFoundException e ) {
            e.printStackTrace();
        } catch ( JAXBException e ) {
            e.printStackTrace();
        }
    }

    @Override
    protected List<TestCases.TestCase> getChildren() {
        return new ArrayList<TestCases.TestCase>( children.keySet() );
    }

    @Override
    protected Description describeChild(TestCases.TestCase testCases) {
        return this.children.get( testCases );
    }

    @Override
    public void run(RunNotifier notifier) {
        TestSuiteContext context = vendorSuite.createContext();
        vendorSuite.beforeTestCases( context, tcd, modelURI );
        super.run( notifier );
        vendorSuite.afterTestCase( context, tcd );
    }

    @Override
    protected void runChild(TestCases.TestCase testCase, RunNotifier runNotifier) {
        Description description = this.children.get( testCase );
        runNotifier.fireTestStarted( description );
        vendorSuite.beforeTest( context, testCase );
        TestResult result = vendorSuite.executeTest( context, testCase );
        switch ( result.getResult() ) {
            case SUCCESS:
                runNotifier.fireTestFinished( description );
                break;
            case IGNORED:
                runNotifier.fireTestIgnored( description );
                break;
            case ERROR:
                runNotifier.fireTestFailure( new Failure( description, new RuntimeException( result.getMsg() ) ) );
                break;
        }
        vendorSuite.afterTest( context, testCase );
    }

    @Override
    public Description getDescription() {
        return descr;
    }

}