package ee.ut.basar;

import java.util.List;
import org.cloudbus.cloudsim.hosts.power.PowerHostSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.vms.Vm;


public class PowerHostExample extends PowerHostSimple {

    public PowerHostExample(long ram, long bw, long storage, List<Pe> peList) {
        super(ram, bw, storage, peList);
    }

    @Override
    public boolean isSuitableForVm(Vm vm) {
        double cap = getVmScheduler().getPeCapacity();
        double mips = vm.getCurrentRequestedMaxMips();
        double pe = vm.getNumberOfPes();
        return getNumberOfFreePes() >= vm.getNumberOfPes() &&
                getVmScheduler().getPeCapacity() >= vm.getCurrentRequestedMaxMips() &&
                getVmScheduler().getAvailableMips() >= vm.getCurrentRequestedTotalMips() &&
                getRamProvisioner().isSuitableForVm(vm, vm.getCurrentRequestedRam()) &&
                getBwProvisioner().isSuitableForVm(vm, vm.getCurrentRequestedBw());
    }

}
