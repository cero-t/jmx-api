package ninja.cero.jvm.jmxapi;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.management.*;
import javax.management.openmbean.CompositeData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.File;
import java.io.IOException;
import java.util.*;

@RestController
public class MbeansApi {
    @RequestMapping("/mbeans/{pid}")
    public Set<ObjectName> mbeans(@PathVariable String pid) throws IOException, AttachNotSupportedException, AgentLoadException, AgentInitializationException {
        JMXConnector connector = getJmxConnector(pid);

        Set<String> set;
        try {
            MBeanServerConnection connection = connector.getMBeanServerConnection();
            return connection.queryNames(null, null);
        } finally {
            connector.close();
        }
    }

    @RequestMapping("/mbeans/{pid}/{name:.+}")
    public MBeanInfo mbeansInfo(@PathVariable String pid, @PathVariable String name)
            throws IOException, AttachNotSupportedException, AgentLoadException, AgentInitializationException, MalformedObjectNameException, IntrospectionException, InstanceNotFoundException, ReflectionException {
        JMXConnector connector = getJmxConnector(pid);

        try {
            MBeanServerConnection connection = connector.getMBeanServerConnection();
            return connection.getMBeanInfo(new ObjectName(name));
        } finally {
            connector.close();
        }
    }

    @RequestMapping("/mbeans/{pid}/{name:.+}/{attributes}")
    public Map<String, Object> mbeansAttribute(@PathVariable String pid, @PathVariable String name, @PathVariable String[] attributes)
            throws IOException, AttachNotSupportedException, AgentLoadException, AgentInitializationException, MalformedObjectNameException, IntrospectionException, InstanceNotFoundException, ReflectionException {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        JMXConnector connector = getJmxConnector(pid);

        try {
            MBeanServerConnection connection = connector.getMBeanServerConnection();
            for (Object o : connection.getAttributes(new ObjectName(name), attributes)) {
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
        } finally {
            connector.close();
        }

        return map;
    }

    protected JMXConnector getJmxConnector(String pid) throws AttachNotSupportedException, IOException, AgentLoadException, AgentInitializationException {
        VirtualMachine vm = VirtualMachine.attach(pid);
        String connectorAddress;
        try {
            connectorAddress = vm.getAgentProperties().getProperty("com.sun.management.jmxremote.localConnectorAddress");

            if (connectorAddress == null) {
                String agent = vm.getSystemProperties().getProperty("java.home") + File.separator + "lib" + File.separator + "management-agent.jar";
                vm.loadAgent(agent);
                connectorAddress = vm.getAgentProperties().getProperty("com.sun.management.jmxremote.localConnectorAddress");
            }
        } finally {
            vm.detach();
        }

        JMXServiceURL serviceURL = new JMXServiceURL(connectorAddress);
        return JMXConnectorFactory.connect(serviceURL);
    }

    protected Map<String, Object> toMap(CompositeData compositeData) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();

        for (String key : compositeData.getCompositeType().keySet()) {
            Object value = compositeData.get(key);
            if (CompositeData.class.isAssignableFrom(value.getClass())) {
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
}
