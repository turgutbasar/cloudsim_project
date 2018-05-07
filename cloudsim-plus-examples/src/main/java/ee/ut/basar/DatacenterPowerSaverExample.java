package ee.ut.basar;

import java.util.Map;
import java.util.Map.Entry;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.Simulation;
import org.cloudbus.cloudsim.datacenters.DatacenterCharacteristics;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.util.Log;
import org.cloudbus.cloudsim.vms.Vm;

public class DatacenterPowerSaverExample extends DatacenterSimple {

    public DatacenterPowerSaverExample(Simulation simulation, DatacenterCharacteristics characteristics, VmAllocationPolicy vmAllocationPolicy) {
        super(simulation, characteristics, vmAllocationPolicy);
    }
    
    public int turn = 0;
    
    

    @Override
    protected double updateCloudletProcessing() {
        turn++;
        final double nextSimulationTime = super.updateCloudletProcessing();
        if (nextSimulationTime == Double.MAX_VALUE){
            return nextSimulationTime;
        }
        
        if ((turn * nextSimulationTime) < 4) {
            return nextSimulationTime;
        }
        turn = 0;
        
        // Remove Finished VMs
        for (PowerHostExample host : this.<PowerHostExample>getHostList()) {
            for (Vm vm : host.getFinishedVms()) {
                if (vm.isInMigration()) {
                    continue;
                }
                getVmAllocationPolicy().deallocateHostForVm(vm);
                Log.printFormattedLine(
                        String.format("%.2f: %s has been deallocated from %s",
                               getSimulation().clock(), vm, host));
            }
        }
        
        final Map<Vm, Host> migrationMap = getVmAllocationPolicy().optimizeAllocation(getVmList());
        // Run optimization process of vmallocation policy
        // Reassign vms and migratevms
        for (final Entry<Vm, Host> entry : migrationMap.entrySet()) {
            migrateVM(entry);
        }
        return nextSimulationTime;
    }
    
    private void migrateVM(Entry<Vm, Host> entry) {
    
        final double currentTime = getSimulation().clock();
        final Host sourceHost = entry.getKey().getHost();
        final Host targetHost = entry.getValue();

        final int migration_delay = 1;

        sourceHost.addVmMigratingOut(entry.getKey());
        targetHost.addMigratingInVm(entry.getKey());

        send(getId(), migration_delay, CloudSimTags.VM_MIGRATE, entry);
    }
    
}