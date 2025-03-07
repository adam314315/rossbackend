/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.framework.utils.file.compression;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Cette classe permet de definir les informations necessaires a la realisation de la compression. Elle possede une
 * donnee membre permettant de stocker la reference de la compression en cours (une instance de classe implementant
 * l'interface ICompression).
 */
public class CompressionContext {

    /**
     * Permet de stocker la ou les ressource(s) a compresser. La source peut etre un fichier ou un repertoire.
     */
    List<File> inputSource = null;

    /**
     * Contient le fichier produit par la compression
     */
    File compressedFile = null;

    /**
     * Contient le repertoire cible de decompression
     */
    File outputDir = null;

    /**
     * Contient le répertoire racine des fichiers à compresser. Utile pour insérer les chemins des fichiers relatifs à
     * une racine (espace utilisateur dédié par exemple)
     */
    File rootDirectory = null;

    /**
     * Designe la strategie de compression
     */
    private ICompression referenceStrategy;

    /**
     * indique si l'archive doit etre cree a plat
     */
    private boolean flatArchive;

    /**
     * Format d'encodage des caracteres lors de la compression
     */
    private Charset charset;

    /**
     * Lancement synchrone ou asynchrone de la compression
     */
    private boolean runInThread;

    /**
     * Constructeur de classe
     */
    protected CompressionContext() {
    }

    /**
     * Cette methode declenche la compression
     *
     * @return le fichier compresse avec l'extension
     * @throws CompressionException Exception survenue lors du traitement
     */
    protected CompressManager doCompress() throws CompressionException {
        return referenceStrategy.compress(inputSource,
                                          compressedFile,
                                          rootDirectory,
                                          flatArchive,
                                          runInThread,
                                          charset);
    }

    /**
     * Cette methode declenche la decompression
     *
     * @throws CompressionException Exception survenue lors du traitement.
     */
    protected void doUncompress() throws CompressionException {
        referenceStrategy.uncompress(compressedFile, outputDir, charset);
    }

    /**
     * Definit le mode de compression
     *
     * @param pCompressionMethod : le mode de compression
     */
    protected void setCompression(ICompression pCompressionMethod) {
        referenceStrategy = pCompressionMethod;
    }

    /**
     * getter pour flatARchive
     */
    protected boolean isFlatArchive() {
        return flatArchive;
    }

    /**
     * Modificateur
     */
    public void setCompressedFile(File pFile) {
        compressedFile = pFile;
    }

    /**
     * Modificateur inputSource
     *
     * @param pList une liste de <code>File</code>
     */
    public void setInputSource(List<File> pList) {
        inputSource = pList;
    }

    /**
     * Modificateur
     */
    public void setOutputDir(File pFile) {
        outputDir = pFile;
    }

    /**
     * @return Returns the rootDirectory.
     */
    public File getRootDirectory() {
        return rootDirectory;
    }

    /**
     * @param pRootDirectory The rootDirectory to set.
     */
    public void setRootDirectory(File pRootDirectory) {
        rootDirectory = pRootDirectory;
    }

    /**
     * setter pour fatARchive_
     */
    protected void setFlatArchive(boolean pFlatArchive) {
        flatArchive = pFlatArchive;
    }

    /**
     * getter charset
     */
    public Charset getCharSet() {
        return charset;
    }

    /**
     * setter charset
     */
    public void setCharSet(Charset charSet) {
        charset = charSet;
    }

    /**
     * Getter runInThread
     */
    public boolean isRunInThread() {
        return runInThread;
    }

    /**
     * Setter runInThread
     */
    public void setRunInThread(boolean pRunInThread) {
        runInThread = pRunInThread;
    }

}
