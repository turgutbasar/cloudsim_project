package ee.ut.basar;

import java.util.ArrayList;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.cloudbus.cloudsim.allocationpolicies.power.PowerVmAllocationPolicyAbstract;

public class PowerVmAllocationPolicyTest extends PowerVmAllocationPolicyAbstract {
    /**
     * Instantiates a new PowerVmAllocationPolicySimple.
     *
     */
    public PowerVmAllocationPolicyTest() {
        super();
    }

    /**
     * The method in this VmAllocationPolicy doesn't perform any
     * VM placement optimization and, in fact, has no effect.
     *
     * @param vmList the list of VMs
     * @return an empty map to indicate that it never performs optimization
     */
    @Override
    public Map<Vm, Host> optimizeAllocation(List<? extends Vm> vmList) {
        // Use Underload and Overload as metrics
        final Map<Vm, Host> migrationMap = new HashMap<>();
        List<Vm> migration = new ArrayList<>();
        List<Host> hostlist = getHostList();
        List<Host> available = new ArrayList<Host>(getHostList());
        for (Host host : hostlist) {
            int hostTotal = 0;
            if (!host.isActive()) {
                continue;
            }
            if (host.getAvailableMips() < 0.2 * host.getTotalMipsCapacity()) {
                migration.addAll(host.getVmList());
                List<Vm> vms = host.getVmList();
                for ( Vm vm : vms) {
                    hostTotal += vm.getNumberOfPes();
                    if (hostTotal > (host.getNumberOfPes() * 0.8)) {
                        break;
                    }
                    migration.remove(vm);
                }
                available.remove(host);
            }
        }
        for (Host host : available) {
            int hostTotal = 0;
            if (!host.isActive()) {
                continue;
            }
            List<Vm> vmsToMigrate = new ArrayList<>();
            if(migration.size() < 1) {
                boolean active = false;
                for (Vm vm : host.getVmList()) {
                    if (!vm.getCloudletScheduler().isEmpty()) {
                        active = true;
                    }
                }
                if (!active) {
                    host.setActive(false);
                    System.out.println("Host Power Off" + host.toString());
                }
            }
            for ( Vm vm : migration) {
                hostTotal += vm.getNumberOfPes();
                if (hostTotal > host.getNumberOfPes() * 0.8) {
                    break;
                }
                vmsToMigrate.add(vm);
            }
            for ( Vm vm : vmsToMigrate) {
                migrationMap.put(vm, host);
                migration.remove(vm);
            }
        }
        if (migration.size() > 0) {
            // Start other VM
            Host tmp = null;
            for (Host host : getHostList()) {
                if (!host.isActive()) {
                    host.setActive(true);
                    tmp = host;
                    break;
                }
            }
            for (Vm vm : migration) {
                if (tmp == null) {
                    break;
                }
                migrationMap.put(vm, tmp);
            }
        }
        return migrationMap;
    }

}
