package ee.ut.basar;

public class PowerVmAllocationPolicyTestFactory {
    
    private Boolean useSimulatedAnnealing = false;
    
    private double alpha = 0.999993;
    
    private double t_criteria = 0.00001;
    
    private double i_criteria = 100000;

    public PowerVmAllocationPolicyTestFactory enableSimulatedAnnealing(Boolean useSimulatedAnnealing) {
        this.useSimulatedAnnealing = useSimulatedAnnealing;
        return this;
    }

    public PowerVmAllocationPolicyTestFactory setAlpha(double alpha) {
        this.alpha = alpha;
        return this;
    }

    public PowerVmAllocationPolicyTestFactory setT_criteria(double t_criteria) {
        this.t_criteria = t_criteria;
        return this;
    }

    public PowerVmAllocationPolicyTestFactory setI_criteria(double i_criteria) {
        this.i_criteria = i_criteria;
        return this;
    }
    
    public PowerVmAllocationPolicyTestFactory() {
        super();
    }
    
    public PowerVmAllocationPolicyTest build() {
        return new PowerVmAllocationPolicyTest(useSimulatedAnnealing, alpha, t_criteria, i_criteria);
    }
    
}