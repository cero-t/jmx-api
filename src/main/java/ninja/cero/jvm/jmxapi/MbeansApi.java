package ninja.cero.jvm.jmxapi;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.management.*;
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
    public MBeanInfo mbeansInfo(@PathVariable String pid, @PathVariable String name) throws IOException, AttachNotSupportedException, AgentLoadException, AgentInitializationException, MalformedObjectNameException, IntrospectionException, InstanceNotFoundException, ReflectionException {
        System.out.println(pid);
        System.out.println(name);
        JMXConnector connector = getJmxConnector(pid);

        try {
            MBeanServerConnection connection = connector.getMBeanServerConnection();

            ObjectName o = new ObjectName(name);
            return connection.getMBeanInfo(o);
        } finally {
            connector.close();
        }
    }

    private static JMXConnector getJmxConnector(String pid) throws AttachNotSupportedException, IOException, AgentLoadException, AgentInitializationException {
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
}
