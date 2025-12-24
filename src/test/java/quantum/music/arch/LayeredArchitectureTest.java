package quantum.music.arch;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;


@AnalyzeClasses(packages = "quantum.music")
public class LayeredArchitectureTest {

    @ArchTest
    static final ArchRule domain_should_be_independent =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("..api..");

    /*@ArchTest
    public static final ArchRule services_should_only_be_accessed_by_resources = classes()
        .that().resideInAPackage("..service..")
        .should().onlyBeAccessed().byAnyPackage("..resource..", "..service..");

    @ArchTest
    public static final ArchRule resources_should_not_access_providers_directly = classes()
        .that().resideInAPackage("..resource..")
        .should().onlyDependOnClassesThat()
        .resideInAnyPackage("..resource..", "..service..", "..dto..", "..model..", "java..", "jakarta..");

    @ArchTest
    public static final ArchRule providers_should_only_be_accessed_by_services = classes()
        .that().resideInAPackage("..providers..")
        .should().onlyBeAccessed().byAnyPackage("..service..", "..providers..");

    @ArchTest
    public static final ArchRule layered_architecture_rule = layeredArchitecture()
        .consideringAllDependencies()
        .layer("Resource").definedBy("..resource..")
        .layer("Service").definedBy("..service..")
        .layer("Provider").definedBy("..providers..")
        .layer("Client").definedBy("..client..")
        .layer("Model").definedBy("..model..", "..dto..")

        .whereLayer("Resource").mayNotBeAccessedByAnyLayer()
        .whereLayer("Resource").mayOnlyAccessLayers("Service", "Model")
        .whereLayer("Service").mayOnlyAccessLayers("Provider", "Client", "Model")
        .whereLayer("Provider").mayOnlyAccessLayers("Client", "Model")
        .whereLayer("Client").mayOnlyAccessLayers("Model");*/

}