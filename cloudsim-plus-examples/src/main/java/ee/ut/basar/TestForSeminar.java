package ee.ut.basar;

import java.util.ArrayList;
import java.util.List;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.datacenters.DatacenterCharacteristics;
import org.cloudbus.cloudsim.datacenters.DatacenterCharacteristicsSimple;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.util.Log;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.ResourceProvisionerSimple;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerSpaceShared;

public class TestForSeminar {

    private final List<Host> hostList;
    
    private final List<Vm> vmList;
    
    private final DatacenterBroker broker;
    
    private final Datacenter datacenter;
    
    private final CloudSim simulation;

    public static void main(String[] args) {
        Log.printFormattedLine("Starting %s ...", TestForSeminar.class.getSimpleName());
        new TestForSeminar();
        Log.printFormattedLine("%s finished!", TestForSeminar.class.getSimpleName());
    }

    public TestForSeminar() {
        
        this.simulation = new CloudSim();
        this.hostList = new ArrayList<>();
        this.vmList = new ArrayList<>();
        this.datacenter = createBasarDatacenter(600, 100);
        this.broker = new DatacenterBrokerSimple(simulation);

        createAndSubmitTestVms(600, 100);
        
        createAndSubmitTestCloudlets();
        
        // TODO : Print table olayını düzeltelim
        runSimulationAndPrintResults();
    }

    private void runSimulationAndPrintResults() {
        this.simulation.start();
        
        /*List<Cloudlet> finishedCloudlets = broker.getCloudletFinishedList();
        new CloudletsTableBuilder(finishedCloudlets).build();*/
    }

    private void createAndSubmitTestCloudlets() {
        for (Vm vm : this.vmList) {
            long length = 1;
            int n_cloudlets = (int)(Math.random()*100);
            long pes = vm.getNumberOfPes() - 1;
            pes = pes == -1 ? 0 : pes;
            for(int i = 0; i < n_cloudlets; i++){
                Cloudlet cloudlet = createCloudlet(vm.getId() * 1000 + i, vm, (int)(Math.random()*length) + 1, (int)(Math.random()*pes) + 1);
                cloudlet.setVm(vm);
                this.broker.submitCloudlet(cloudlet);
            }
        }
    }

    private void createAndSubmitTestVms(int pes, int mips) {
        int i = 0;
        int time_interval = 60;
        while (pes > 0) {
            List vv = new ArrayList();
            int pes_vm;
            if (pes < 11) {
                pes_vm = pes;
            } else {
                pes_vm = (int)(Math.random() * 10) + 1;
            }
            pes -= pes_vm;
            Vm vm = createVm(i, pes_vm, mips);
            int delay = (int)(Math.random()*time_interval);
            vv.add(vm);
            this.broker.submitVmList(vv, delay);
            vmList.add(vm);
            ++i;
        }
        
    }

    private Vm createVm(int id, int pes, int mips) {
        long size = 10; // image size (MEGABYTE)
        int ram = 5; // vm memory (MEGABYTE)
        long bw = 10;
        return new VmSimple(id, mips, pes)
            .setRam(ram).setBw(bw).setSize(size)
            .setCloudletScheduler(new CloudletSchedulerSpaceShared());
    }

    private Cloudlet createCloudlet(int id, Vm vm, long length, int pes) {
        long fileSize = 300;
        long outputSize = 300;
        return new CloudletSimple(id, (int)(Math.random()*length) + 1, pes)
                    .setFileSize(fileSize)
                    .setOutputSize(outputSize)
                    .setUtilizationModel(new UtilizationModelFull())
                    .setVm(vm);
    }

    private Host createHost(int id, int pes, int mips) {
        List<Pe> peList = new ArrayList<>();
        for(int i = 0; i < pes; i++){
            peList.add(new PeSimple(mips, new PeProvisionerSimple()));
        } // Number of PEs
        long ram = 2048; // host memory (MEGABYTEs)
        long storage = 1000000; // host storage (MEGABYTEs)
        long bw = 10000; //Megabits/s

        return new PowerHostExample(ram, bw, storage, peList)
            .setRamProvisioner(new ResourceProvisionerSimple())
            .setBwProvisioner(new ResourceProvisionerSimple())
            .setVmScheduler(new VmSchedulerTimeShared(0.0))
            .setActive(false);
    }

    private Datacenter createBasarDatacenter(int pes, int mips) {
        int i = 0;
        while (pes > 0) {
            int pes_host;
            if (pes < 40) {
                pes_host = pes;
            } else {
                pes_host = (int)(Math.random() * 30) + 10;
            }
            pes -= pes_host;
            this.hostList.add(createHost(i, pes_host, mips));
            ++i;
        }
        DatacenterCharacteristics characteristics = new DatacenterCharacteristicsSimple(hostList);
        return new DatacenterPowerSaverExample(simulation, characteristics, new PowerVmAllocationPolicyTestFactory().enableSimulatedAnnealing(Boolean.TRUE).build());
    }
}