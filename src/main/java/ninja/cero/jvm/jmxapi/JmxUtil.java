package ninja.cero.jvm.jmxapi;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

public class JmxUtil {
    static Map<String, VirtualMachine> vmCache = new ConcurrentHashMap<String, VirtualMachine>();
    static Map<String, JMXConnector> connectorChache = new ConcurrentHashMap<String, JMXConnector>();


    public List<Jps> jps() {
        List<Jps> jpsList = new ArrayList<Jps>();

        List<VirtualMachineDescriptor> vmList = VirtualMachine.list();
        for (VirtualMachineDescriptor vm : vmList) {
            Jps jps = new Jps();
            jps.name = vm.displayName();
            jps.pid = vm.id();
            jps.simpleName = toSimpleName(jps.name);
            jpsList.add(jps);
        }

        return jpsList;
    }

    protected String toSimpleName(String name) {
        String[] values = name.split(" ");
        if (values.length == 0) {
            return "";
        }

        values = values[0].split("\\.");
        if (values.length == 0) {
            return "";
        }

        return values[values.length - 1];
    }

    public static class Jps {
        public String pid;
        public String name;
        public String simpleName;
    }

    public Jinfo jinfo(String pid) throws IOException, AttachNotSupportedException {
        VirtualMachine vm = getVirutualMachine(pid);

        Jinfo jinfo = new Jinfo();
        jinfo.systemProperties = vm.getSystemProperties();
        jinfo.agentProperties = vm.getAgentProperties();

        return jinfo;
    }

    public static class Jinfo {
        public Map<Object, Object> systemProperties;
        public Map<Object, Object> agentProperties;
    }

    public Set<String> mbeans(String pid) throws IOException, AttachNotSupportedException, AgentLoadException, AgentInitializationException {
        JMXConnector connector = getJmxConnector(pid);
        MBeanServerConnection connection = connector.getMBeanServerConnection();

        Set<String> set = new TreeSet<String>();
        Set<ObjectName> names = connection.queryNames(null, null);
        for (ObjectName name : names) {
            set.add(name.getCanonicalName());
        }

        return set;
    }

    public Map<String, Object> mbeansInfo(String pid, String name)
            throws IOException, AttachNotSupportedException, AgentLoadException, AgentInitializationException, MalformedObjectNameException, IntrospectionException, InstanceNotFoundException, ReflectionException {
        JMXConnector connector = getJmxConnector(pid);
        MBeanServerConnection connection = connector.getMBeanServerConnection();

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        MBeanInfo mBeanInfo = connection.getMBeanInfo(new ObjectName(name));

        result.put("description", mBeanInfo.getDescription());
        result.put("interfaceClassName", mBeanInfo.getDescriptor().getFieldValue("interfaceClassName"));

        List<Map<String, Object>> attributes = new ArrayList<Map<String, Object>>();
        for (MBeanAttributeInfo attribute : mBeanInfo.getAttributes()) {
            Map<String, Object> attributeMap = new LinkedHashMap<String, Object>();
            attributeMap.put("name", attribute.getName());
            attributeMap.put("type", toReadableType(attribute.getType()));
            attributeMap.put("readable", attribute.isReadable());
            attributeMap.put("writable", attribute.isWritable());
            attributes.add(attributeMap);
        }
        result.put("attributes", attributes);

        List<Map<String, Object>> operations = new ArrayList<Map<String, Object>>();
        for (MBeanOperationInfo operation : mBeanInfo.getOperations()) {
            Map<String, Object> operationMap = new LinkedHashMap<String, Object>();
            operationMap.put("name", operation.getName());

            List<String> argType = new ArrayList<String>();
            for (MBeanParameterInfo parameterInfo : operation.getSignature()) {
                argType.add(toReadableType(parameterInfo.getType()));
            }
            operationMap.put("argTypes", argType);
            operationMap.put("returnType", toReadableType(operation.getReturnType()));

            operations.add(operationMap);
        }
        result.put("operations", operations);

        List<String> constructors = new ArrayList<String>();
        for (MBeanConstructorInfo constructor : mBeanInfo.getConstructors()) {
            constructors.add(constructor.getName());
        }
        result.put("constructors", constructors);

        List<String> notifications = new ArrayList<String>();
        for (MBeanNotificationInfo notification : mBeanInfo.getNotifications()) {
            notifications.add(notification.getName());
        }
        result.put("notifications", notifications);

        return result;
    }

    protected Map<String, Object> mbeansWriteAttribute(String pid, String name, Map<String, String> params)
            throws IOException, AttachNotSupportedException, AgentLoadException, AgentInitializationException, MalformedObjectNameException, IntrospectionException, InstanceNotFoundException, ReflectionException {
        JMXConnector connector = getJmxConnector(pid);
        MBeanServerConnection connection = connector.getMBeanServerConnection();
        ObjectName objectName = new ObjectName(name);

        MBeanInfo mBeanInfo = connection.getMBeanInfo(objectName);
        MBeanAttributeInfo[] attributeInfos = mBeanInfo.getAttributes();

        AttributeList updateAttributes = new AttributeList();
        for (MBeanAttributeInfo attributeInfo : attributeInfos) {
            if (params.containsKey(attributeInfo.getName())) {
                Object value = converType(params.get(attributeInfo.getName()), attributeInfo.getType());
                updateAttributes.add(new Attribute(attributeInfo.getName(), value));
            }
        }

        AttributeList attributes = connection.setAttributes(new ObjectName(name), updateAttributes);
        return attribtesToMap(attributes);
    }

    public Object mbeansAttributeOrInvoke(String pid, String name, String[] keys, Map<String, String> params) throws IOException, AttachNotSupportedException, AgentLoadException, AgentInitializationException, MalformedObjectNameException, ReflectionException, InstanceNotFoundException, IntrospectionException, MBeanException {
        JMXConnector connector = getJmxConnector(pid);
        MBeanServerConnection connection = connector.getMBeanServerConnection();
        ObjectName objectName = new ObjectName(name);

        AttributeList attributes = connection.getAttributes(objectName, keys);
        if (attributes.isEmpty()) {
            if (keys.length == 1) {
                return mbeansInvoke(connection, objectName, keys[0], params);
            } else {
                throw new IllegalArgumentException("Attributes not found or operation number is more than one : " + Arrays.toString(keys));
            }
        } else {
            return attribtesToMap(attributes);
        }
    }

    protected Map<String, Object> attribtesToMap(AttributeList attributes)
            throws IOException, AttachNotSupportedException, AgentLoadException, AgentInitializationException, MalformedObjectNameException, IntrospectionException, InstanceNotFoundException, ReflectionException {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        for (Object o : attributes) {
            if (Attribute.class.isAssignableFrom(o.getClass())) {
                Attribute attribute = (Attribute) o;
                Object value = attribute.getValue();

                if (CompositeData.class.isAssignableFrom(value.getClass())) {
                    map.put(attribute.getName(), toMap((CompositeData) value));
                } else if (CompositeData[].class.isAssignableFrom(value.getClass())) {
                    map.put(attribute.getName(), toList((CompositeData[]) value));
                } else {
                    map.put(attribute.getName(), value);
                }
            }
        }

        return map;
    }

    protected Object mbeansInvoke(MBeanServerConnection connection, ObjectName objectName, String operation, Map<String, String> params)
            throws IOException, AttachNotSupportedException, AgentLoadException, AgentInitializationException, MalformedObjectNameException, IntrospectionException, InstanceNotFoundException, ReflectionException, MBeanException {
        MBeanInfo mBeanInfo = connection.getMBeanInfo(objectName);
        Collection<String> values = params.values();
        Map<Object, String> map = toArgs(operation, values.toArray(new String[values.size()]), mBeanInfo);

        Set<Object> args = map.keySet();
        Collection<String> signatures = map.values();

        Object result = connection.invoke(objectName, operation, args.toArray(new Object[args.size()]), signatures.toArray(new String[signatures.size()]));
        if (CompositeData.class.isAssignableFrom(result.getClass())) {
            return toMap((CompositeData) result);
        } else if (CompositeData[].class.isAssignableFrom(result.getClass())) {
            return toList((CompositeData[]) result);
        } else {
            return result;
        }
    }

    public synchronized void close() {
        for (VirtualMachine vm : vmCache.values()) {
            try {
                vm.detach();
            } catch (IOException e) {
                // continue to detach
                e.printStackTrace();
            }
        }

        for (JMXConnector connector : connectorChache.values()) {
            try {
                connector.close();
            } catch (IOException e) {
                // continue to close
                e.printStackTrace();
            }
        }
    }

    protected VirtualMachine getVirutualMachine(String pid) throws IOException, AttachNotSupportedException {
        synchronized (pid.intern()) {
            VirtualMachine vm = vmCache.get(pid);
            if (vm == null) {
                vm = VirtualMachine.attach(pid);
                vmCache.put(pid, vm);
            }
            return vm;
        }
    }

    protected JMXConnector getJmxConnector(String pid) throws AttachNotSupportedException, IOException, AgentLoadException, AgentInitializationException {
        synchronized (pid.intern()) {
            JMXConnector connector = connectorChache.get(pid);
            if (connector != null) {
                try {
                    connector.getConnectionId();
                    return connector;
                } catch (IOException e) {
                    // VM is stopped or connector is decayed
                    vmCache.remove(pid);
                    connectorChache.remove(pid);
                }
            }

            VirtualMachine vm = getVirutualMachine(pid);

            String connectorAddress = vm.getAgentProperties().getProperty("com.sun.management.jmxremote.localConnectorAddress");
            if (connectorAddress == null) {
                String agent = vm.getSystemProperties().getProperty("java.home") + File.separator + "lib" + File.separator + "management-agent.jar";
                vm.loadAgent(agent);
                connectorAddress = vm.getAgentProperties().getProperty("com.sun.management.jmxremote.localConnectorAddress");
            }

            JMXServiceURL serviceURL = new JMXServiceURL(connectorAddress);
            connector = JMXConnectorFactory.connect(serviceURL);

            connectorChache.put(pid, connector);

            return connector;
        }
    }

    protected Map<String, Object> toMap(CompositeData compositeData) {
        if (compositeData == null) {
            return null;
        }

        Map<String, Object> map = new LinkedHashMap<String, Object>();

        for (String key : compositeData.getCompositeType().keySet()) {
            Object value = compositeData.get(key);

            if (value != null && CompositeData.class.isAssignableFrom(value.getClass())) {
                value = toMap((CompositeData) value);
            }

            map.put(key, value);
        }

        return map;
    }

    protected List<Map<String, Object>> toList(CompositeData[] compositeData) {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>(compositeData.length);

        for (CompositeData c : compositeData) {
            list.add(toMap(c));
        }

        return list;
    }

    protected Map<Object, String> toArgs(String opertionName, String[] values, MBeanInfo mBeanInfo) {
        MBeanOperationInfo target = null;
        MBeanOperationInfo namedTarget = null;

        OUT:
        for (MBeanOperationInfo operationInfo : mBeanInfo.getOperations()) {
            if (!operationInfo.getName().equals(opertionName)) {
                continue;
            }
            namedTarget = operationInfo;

            MBeanParameterInfo[] signature = operationInfo.getSignature();
            if (signature.length != values.length) {
                continue;
            }

            for (int i = 0; i < signature.length; i++) {
                if (signature[i].getType().startsWith("[") && !values[i].contains(",")) {
                    continue OUT;
                }

                if (!signature[i].getType().startsWith("[") && values[i].contains(",")) {
                    continue OUT;
                }
            }

            target = operationInfo;
            break;
        }

        if (namedTarget == null) {
            throw new IllegalArgumentException("Target attribute or operation not found.");
        }

        if (target == null) {
            throw new IllegalArgumentException("Argument size or type is not expected.");
        }

        MBeanParameterInfo[] signature = target.getSignature();

        Map<Object, String> args = new LinkedHashMap<Object, String>(signature.length);
        for (int i = 0; i < signature.length; i++) {
            Object converted = converType(values[i], signature[i].getType());
            args.put(converted, signature[i].getType());
        }

        return args;
    }

    protected Object converType(String value, String type) {
        if (type.equals("java.lang.String")) {
            return value;
        } else if (type.equals("boolean")) {
            return Boolean.valueOf(value);
        } else if (type.equals("byte")) {
            return Byte.valueOf(value);
        } else if (type.equals("char")) {
            return value.toCharArray()[0];
        } else if (type.equals("double")) {
            return Double.valueOf(value);
        } else if (type.equals("float")) {
            return Float.valueOf(value);
        } else if (type.equals("int")) {
            return Integer.valueOf(value);
        } else if (type.equals("long")) {
            return Long.valueOf(value);
        } else if (type.equals("short")) {
            return Short.valueOf(value);
        } else if (type.startsWith("[")) {
            String[] values = value.split(",");
            return convertArray(values, type.substring(1));
        }

        throw new IllegalArgumentException("Unknown Type : " + type);
    }

    protected Object convertArray(String[] values, String type) {
        if (type.equals("Ljava.lang.String;")) {
            return values;
        } else if (type.equals("Z")) {
            boolean[] returnValues = new boolean[values.length];
            for (int i = 0; i < returnValues.length; i++) {
                returnValues[i] = Boolean.parseBoolean(values[i]);
            }
            return returnValues;
        } else if (type.equals("B")) {
            byte[] returnValues = new byte[values.length];
            for (int i = 0; i < returnValues.length; i++) {
                returnValues[i] = Byte.parseByte(values[i]);
            }
            return returnValues;
        } else if (type.equals("C")) {
            char[] returnValues = new char[values.length];
            for (int i = 0; i < returnValues.length; i++) {
                returnValues[i] = values[i].toCharArray()[0];
            }
            return returnValues;
        } else if (type.equals("D")) {
            double[] returnValues = new double[values.length];
            for (int i = 0; i < returnValues.length; i++) {
                returnValues[i] = Double.parseDouble(values[i]);
            }
            return returnValues;
        } else if (type.equals("F")) {
            float[] returnValues = new float[values.length];
            for (int i = 0; i < returnValues.length; i++) {
                returnValues[i] = Float.parseFloat(values[i]);
            }
            return returnValues;
        } else if (type.equals("I")) {
            int[] returnValues = new int[values.length];
            for (int i = 0; i < returnValues.length; i++) {
                returnValues[i] = Integer.parseInt(values[i]);
            }
            return returnValues;
        } else if (type.equals("J")) {
            long[] returnValues = new long[values.length];
            for (int i = 0; i < returnValues.length; i++) {
                returnValues[i] = Long.parseLong(values[i]);
            }
            return returnValues;
        } else if (type.equals("S")) {
            short[] returnValues = new short[values.length];
            for (int i = 0; i < returnValues.length; i++) {
                returnValues[i] = Short.parseShort(values[i]);
            }
            return returnValues;
        }

        throw new IllegalArgumentException("Unknown Type : " + type);
    }

    protected String toReadableType(String type) {
        if (!type.startsWith("[")) {
            return type;
        } else if (type.startsWith("[L")) {
            return type.substring(2, type.length() - 1) + "[]";
        } else if (type.equals("[Z")) {
            return "boolean[]";
        } else if (type.equals("[B")) {
            return "byte[]";
        } else if (type.equals("[C")) {
            return "char[]";
        } else if (type.equals("[D")) {
            return "double[]";
        } else if (type.equals("[F")) {
            return "float[]";
        } else if (type.equals("[I")) {
            return "int[]";
        } else if (type.equals("[J")) {
            return "long[]";
        } else if (type.equals("[S")) {
            return "short[]";
        }

        throw new IllegalArgumentException("Unknown Type : " + type);
    }
}
