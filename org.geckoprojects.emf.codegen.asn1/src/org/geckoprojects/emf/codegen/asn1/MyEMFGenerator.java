package org.geckoprojects.emf.codegen.asn1;
import java.io.IOException;
import java.util.Collections;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;

public class MyEMFGenerator
{
   public void generate(final String pathToDDLEcore, final String pathToOutputFile) throws IOException
   {
       // it is very important that we do everything through ResourceSet's
       ResourceSet resourceSet = new ResourceSetImpl();
       Resource ddlResource = createAndLoadDDLResource(resourceSet, pathToDDLEcore);

       // of course, in production code we would fail here if there were no
       // contents or they weren't of type EPackage.
       final EPackage ddlPackage = (EPackage) ddlResource.getContents().get(0);

       // next we will create our own new package to contained what we will generate
       final EPackage newPackage = createPackage("customerDB", "customerDB", "http://customerDB");

       // next, create the row class
       EClass customerRow = createEClass("CustomerRow");
       // add to package before we do anything else
       newPackage.getEClassifiers().add(customerRow);
       // add our super-class
       addSuperType(customerRow, ddlPackage, "AbstractRow");
       // add our features
       addAttribute(customerRow, "id", EcorePackage.Literals.ESTRING, true, 1, 1);
       addAttribute(customerRow, "firstName", EcorePackage.Literals.ESTRING, false, 0, 1);
       addAttribute(customerRow, "lastName", EcorePackage.Literals.ESTRING, false, 0, 1);

       // next, create the table class
       EClass customers = createEClass("Customers");
       // add to package before we do anything else
       newPackage.getEClassifiers().add(customers);
       // add our super-class
       addSuperType(customers, ddlPackage, "AbstractTable");
       // add our features
       addReference(customers, "rows", customerRow, 0, -1);

       // now create a new resource to serialize the ecore model
       Resource outputRes = resourceSet.createResource(URI.createFileURI(pathToOutputFile));
       // add our new package to resource contents
       outputRes.getContents().add(newPackage);
       // and at last, we save to standard out.  Remove the first argument to save to file specified in pathToOutputFile
       outputRes.save(System.out, Collections.emptyMap());
   }

   private void addAttribute(EClass customerRow, String name,
           EClassifier type, boolean isId, int lowerBound, int upperBound)
   {
       final EAttribute attribute = EcoreFactory.eINSTANCE.createEAttribute();
       // always add to container first
       customerRow.getEStructuralFeatures().add(attribute);
       attribute.setName(name);
       attribute.setEType(type);
       attribute.setID(isId);
       attribute.setLowerBound(lowerBound);
       attribute.setUpperBound(upperBound);
   }

   private void addReference(EClass customerRow, String name,
           EClassifier type, int lowerBound, int upperBound)
   {
       final EReference reference = EcoreFactory.eINSTANCE.createEReference();
       // always add to container first
       customerRow.getEStructuralFeatures().add(reference);
       reference.setName(name);
       reference.setEType(type);
       reference.setLowerBound(lowerBound);
       reference.setUpperBound(upperBound);
   }

   private EPackage createPackage(final String name, final String prefix,
           final String uri)
   {
       final EPackage epackage = EcoreFactory.eINSTANCE.createEPackage();
       epackage.setName(name);
       epackage.setNsPrefix(prefix);
       epackage.setNsURI(uri);
       return epackage;
   }
   
   private EClass createEClass(final String name)
   {
       final EClass eClass = EcoreFactory.eINSTANCE.createEClass();
       eClass.setName(name);
       return eClass;
   }

   private void addSuperType(EClass customerRow, EPackage ddlPackage,
           String name)
   {
        final EClass eSuperClass = (EClass) ddlPackage.getEClassifier(name);
        customerRow.getESuperTypes().add(eSuperClass);
   }

   private Resource createAndLoadDDLResource(ResourceSet resourceSet,
           String pathToDDLEcore) throws IOException
   {
       // creating a proper URI is vitally important since this is how
       // referenced objects in the is ecore file will be found from the ecore
       // file that we produce.
       final URI uri = URI.createFileURI(pathToDDLEcore);
       Resource res = resourceSet.createResource(uri);
       res.load(Collections.emptyMap());
       return res;
   }
 
   public static void main(String[] args) throws IOException
   {
       if (args.length < 1)
       {
           System.err.println("Usage: java MyEMFGenerator <pathToDDLEcore> <outputPath>");
       }

       final String pathToDDLEcore = args[0];
       final String outputPath = args[1];

       // note: these are extra steps that are need from main, but not if running inside Eclipse
       initStandalone();

        new MyEMFGenerator().generate(pathToDDLEcore, outputPath);
   }

   private static void initStandalone()
   {
       Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("ecore", new EcoreResourceFactoryImpl());
       // NOTE: It is very important that you DO NOT make the call to any
       // EPackage.eINSTANCE's here.  Unlike in the FAQ example, we want to be sure
       // that all non-plugin EPackages are loaded directly from ecore files, not generated
       // Java classes.
   }
}