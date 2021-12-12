import javassist.ClassPool;
import org.apache.maven.shared.invoker.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.List;

public class Main {

    public static void main(String[] args) throws FileNotFoundException {
        ClassPool cp2 = ClassPool.getDefault();


        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(new File( "L:\\Secret_Projects\\Java\\MiniLibWorkspace\\Client\\pom.xml" ));
        request.setGoals(Collections.singletonList("clean install"));

        Invoker invoker = new DefaultInvoker();
        invoker.setMavenHome(new File("C:\\Program Files\\Maven\\apache-maven-3.8.3"));
        invoker.setLocalRepositoryDirectory(new File("C:\\Users\\Loukas\\Desktop\\new"));

        try {
            invoker.execute(request);
        } catch (MavenInvocationException e) {
            e.printStackTrace();
        }

//        System.out.println(request.getBaseDirectory());
//        System.out.println(request.getGlobalSettingsFile());
//        System.out.println(request.getJavaHome());
//        System.out.println(request.getGlobalToolchainsFile());
//        System.out.println(request.getMavenOpts());
//        System.out.println(request.getPomFile());
//        System.out.println(request.getPomFileName());
//        System.out.println(request.getProfiles());
//        System.out.println(request.getProjects());
//        System.out.println(request.getResumeFrom());
//        System.out.println(request.getProperties());

        ClassCreatorOld cc = new ClassCreatorOld("HardcodedTest");

        StringBuilder my_method = new StringBuilder();
        my_method.append("public void ")
                .append("test_method")
                .append("() {")
                .append("System.out.println();")
                .append("}");

        cc.addMethod(my_method);
        cc.createClassFile();
        System.out.println("Testing hardcoded method -> Done");


        FileFinder jf = new FileFinder();
        //List<File> jar_list = jf.findAll("C:\\Users\\Loukas\\.m2", ".jar");
        List<File> jar_list = jf.findAll("C:\\Users\\Loukas\\Desktop\\new", ".jar");

        //!important
        jar_list.add(new File("L:\\Secret_Projects\\MiniLib_workspace\\Client\\out\\artifacts\\Client_jar\\Client.jar"));

        ClassParser cp = new ClassParser();
        //List<String> classes_list = cp.getClasses("L:\\Secret_Projects\\Java\\MiniLibWorkspace\\Client\\out\\artifacts\\Client_jar\\Client.jar");
        List<String> classes_list = cp.getClasses(jar_list);

        //!important
        //jar_list.add(new File("L:\\Secret_Projects\\Java\\MiniLibWorkspace\\Client\\out\\artifacts\\Client_jar\\Client.jar"));

        ClassInsider ci = new ClassInsider(jar_list);
        //ClassInsider ci = new ClassInsider("L:\\Secret_Projects\\Java\\MiniLibWorkspace\\Client\\out\\artifacts\\Client_jar\\Client.jar");
        ci.assignStartingMethod("Main", "main");
        try {
            ci.listCalledMethods(classes_list);
        }catch (Exception e) {
            e.printStackTrace();
        }


        MethodElement method_tree = ci.getRoot();
        System.out.println("Visiting -> Done");

        try {
            TreePrinterVisitor tree_printer = new TreePrinterVisitor("ast");
            tree_printer.openWriteGraph();
            ci.getRoot().accept(tree_printer);
            tree_printer.closeWriteGraph();
            System.out.println("Printing -> Done");
        }catch (Exception e) {
            e.printStackTrace();
        }

        try {
            ClassGeneratorVisitor cgv = new ClassGeneratorVisitor("Generated");
            ci.getRoot().accept(cgv);
            //cgv.createClassFile();
            System.out.println("Generating -> Done");
        }catch (Exception e) {
            e.printStackTrace();
        }

    }
}
