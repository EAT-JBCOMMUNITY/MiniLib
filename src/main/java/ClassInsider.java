import javassist.expr.*;
import models.ClassInfo;
import javassist.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class ClassInsider {

    private ClassPool class_pool;
    private String starting_class;
    private String starting_method;
    private CtClass[] params;
    private MethodElement root = null;
    private MethodElement current_parent = null;
    private Stack<MethodElement> parents_stack = new Stack<>();

    private List<String> keep_only;
    private List<String> one_time;

    public ClassInsider(String jar_path) {
        class_pool = ClassPool.getDefault();

        try {
            class_pool.appendClassPath(jar_path);
            //class_pool.insertClassPath(jar_path);
        }catch (NotFoundException e) {
            e.printStackTrace();
        }
    }

    public ClassInsider(List<File> jar_list) {
        class_pool = ClassPool.getDefault();
        try {
            for (File jar_file : jar_list) {
                class_pool.insertClassPath(jar_file.getAbsolutePath());
            }
        }catch (NotFoundException e) {
            e.printStackTrace();
        }
    }

    public MethodElement getRoot() {
        return root;
    }

    public void assignStartingMethod(String class_name, String starting_method) {
        this.starting_class = class_name;
        this.starting_method = starting_method;

        try {
            this.params = class_pool.getCtClass(starting_class).getDeclaredMethod(starting_method).getParameterTypes();
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
    }

    public List listCalledMethods() {
        List<ClassInfo> m_list = new ArrayList<>();
//        parents_stack.push(root);
        //this.listCalledMethodsRecursive(this.starting_class, this.starting_method, m_list);
        this.listCalledMethodsRecursive(this.starting_class, this.starting_method, this.params, Enum.ExprCall.METHOD_CALL, m_list);
        return m_list;
    }

    public List listCalledMethods(List<String> keep_only) throws NotFoundException {
        this.keep_only = keep_only;
        this.one_time = new ArrayList<>();

        List<ClassInfo> m_list = new ArrayList<>();
//        parents_stack.push(root);
        this.listCalledMethodsRecursive(this.starting_class, this.starting_method, this.params, Enum.ExprCall.METHOD_CALL, m_list);
        //this.listCalledMethodsRecursive(this.starting_class, this.starting_method, null, 2);
        return m_list;
    }

//    private void listCalledMethodsRecursive(String class_name, String starting_method, CtClass[] params, int type) {
//        try {
//            CtClass ct_class = class_pool.get(class_name);
//            CtBehavior method;
//            if(type == 1) {
//                method = ct_class.getDeclaredConstructor(params);
//            }else{
//                method = ct_class.getDeclaredMethod(starting_method);
//            }
//            method.instrument(
//                    new ExprEditor() {
//                        @Override
//                        public void edit(NewExpr e) throws CannotCompileException {
//                            //super.edit(e);
//                            try {
//                                System.out.println(e.getConstructor().getName());
//                                listCalledMethodsRecursive(e.getConstructor().getLongName().replace("()", ""), e.getConstructor().getName(), e.getConstructor().getParameterTypes(),1);
//                            } catch (NotFoundException ex) {
//                                ex.printStackTrace();
//                            }
//                        }
//
//                        @Override
//                        public void edit(NewArray a) throws CannotCompileException {
//                            //super.edit(a);
//                            System.out.println(a.getCreatedDimensions());
//                        }
//
//                        @Override
//                        public void edit(ConstructorCall c) throws CannotCompileException {
//                           // super.edit(c);
//                            System.out.println(c.getMethodName());
//                            //listCalledMethodsRecursive(c.get, e.getConstructor().getName());
//                        }
//
//                        @Override
//                        public void edit(MethodCall m) throws CannotCompileException {
//                            //super.edit(m);
//                            System.out.println(m.getMethodName());
//                            try {
//                                listCalledMethodsRecursive(m.getClassName(), m.getMethodName(), m.getMethod().getParameterTypes(), 2);
//                            } catch (NotFoundException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                    });
//        }catch (NotFoundException | CannotCompileException e) {
//            e.printStackTrace();
//        }
//    }

    private void listCalledMethodsRecursive(String class_name, String starting_method, CtClass[] params, Enum.ExprCall type, List list) {
        if (root != null) {
            current_parent = parents_stack.peek();
        }

        try {
            CtClass ct_class = class_pool.get(class_name);

            CtBehavior behavior = null;
            switch(type) {
                case METHOD_CALL:
                    behavior = ct_class.getDeclaredMethod(starting_method, params);
                    break;
                case CONSTRUCTOR_CALL:
                    behavior = ct_class.getDeclaredConstructor(params);
                    break;
            }

            behavior.instrument(
                new ExprEditor() {
                    @Override
                    public void edit(NewExpr e) {
                        try {
                            CtConstructor constructor = e.getConstructor();
                            String inline_info = constructor.getName();
                            //System.out.println(inline_info);
                            if (!constructor.isEmpty()) {
                                listCalledMethodsRecursive(constructor.getLongName().replaceAll("\\([^)]*\\)", ""), constructor.getName(), constructor.getParameterTypes(), Enum.ExprCall.CONSTRUCTOR_CALL, list);
                            }
                        } catch (NotFoundException ex) {
                            ex.printStackTrace();
                        }
                    }

                    @Override
                    public void edit(NewArray a) throws CannotCompileException {
                        super.edit(a);
                    }

                    @Override
                    public void edit(MethodCall m) throws CannotCompileException {
                        String inline_info = m.getClassName() + "+" + m.getMethodName() + "+" + m.getSignature();
                        //System.out.println(inline_info);
                        try {
                            addToTree(m, m.getMethod().getParameterTypes(), Enum.ExprCall.METHOD_CALL, list);
                        } catch (NotFoundException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void edit(ConstructorCall c) {
                        String inline_info = c.getClassName() + "+" + c.getMethodName() + "+" + c.getSignature();
                        //System.out.println(inline_info);
                        try {
                            addToTree(c, c.getConstructor().getParameterTypes(), Enum.ExprCall.CONSTRUCTOR_CALL, list);
                        } catch (NotFoundException e) {
                            e.printStackTrace();
                        }
                    }

                });
        }catch (NotFoundException | CannotCompileException e) {
            e.printStackTrace();
        }
    }

    private void addToTree(MethodCall method_call, CtClass[] params, Enum.ExprCall call_type, List list) throws NotFoundException {
        if(keep_only != null && keep_only.contains(method_call.getClassName())) {
            String inline_info = method_call.getClassName() + "+" + method_call.getMethodName() + "+" + method_call.getSignature();
            if(!one_time.contains(inline_info)) {
                one_time.add(inline_info);
                System.out.println(inline_info);
                MethodElement new_node = new MethodElement(method_call.getClassName(), method_call.getMethodName(), params, method_call.getSignature(), Enum.Methods.METHOD);
                if (root == null) {
                    root = new_node;
                    parents_stack.push(root);
                    current_parent = new_node; // = parents_stack.peek();
                } else {
                    current_parent.addChild(new_node);
                    parents_stack.push(new_node);
                }
            }else{
                return;
            }

            listCalledMethodsRecursive(method_call.getClassName(), method_call.getMethodName(), params, call_type,  list);
            parents_stack.pop();
        }
    }
}
