package eu.stratuslab.marketplace.metadata;

import java.io.InputStream;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelMaker;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileManager;

public class CheckRdf {

    /**
     * @param args
     */
    public static void main(String[] args) {

        // create an empty model
        ModelMaker maker = ModelFactory.createMemModelMaker();

        String inputFileName = "ttylinux-9.7-i486-base-1.0.xml";
        String imageIdentifier = "MMZu9WvwKIro-rtBQfDk4PsKO7_";
        Model model = maker.createModel(imageIdentifier);

        // use the FileManager to find the input file
        InputStream in = FileManager.get().open(inputFileName);
        if (in == null) {
            throw new IllegalArgumentException("File: " + inputFileName
                    + " not found");
        }
        // read the RDF/XML file
        model.read(in, "");

    }

}
