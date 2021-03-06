package com.vitimage.common;

import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.List;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.stream.Collectors;

import org.itk.simple.DisplacementFieldTransform;
import org.itk.simple.GradientImageFilter;
import org.itk.simple.Image;
import org.itk.simple.OtsuThresholdImageFilter;
import org.itk.simple.RecursiveGaussianImageFilter;
import org.itk.simple.ResampleImageFilter;
import org.scijava.java3d.Transform3D;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Point3f;

import com.vitimage.common.TransformUtils.AngleComparator;
import com.vitimage.common.TransformUtils.VolumeComparator;
import com.vitimage.registration.ItkTransform;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.Overlay;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.gui.TextRoi;
import ij.measure.Calibration;
import ij.plugin.ChannelSplitter;
import ij.plugin.Concatenator;
import ij.plugin.Duplicator;
import ij.plugin.ImageCalculator;
import ij.plugin.RGBStackMerge;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.ThresholdToSelection;
import ij.plugin.frame.RoiManager;
import ij.process.ByteProcessor;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import ij.process.StackConverter;
import inra.ijpb.binary.geodesic.GeodesicDistanceTransform3DFloat;
import inra.ijpb.morphology.Morphology;
import inra.ijpb.morphology.Strel3D;
import math3d.Point3d;
import mcib3d.image3d.ImageHandler;
import mcib3d.image3d.processing.CannyEdge3D;
import trainableSegmentation.FeatureStack;
import trainableSegmentation.WekaSegmentation;
import vib.BenesNamedPoint;

public interface VitimageUtils {
	//Four constants from http://www.tomgibara.com/computer-vision/CannyEdgeDetector.java
	public final static float GAUSSIAN_CUT_OFF = 0.005f;
	public final static float MAGNITUDE_SCALE = 100F;
	public final static float MAGNITUDE_LIMIT = 1000F;
	public final static int MAGNITUDE_MAX = (int) (MAGNITUDE_SCALE * MAGNITUDE_LIMIT);
	public final static double EPSILON=0.00001;
	public static int OP_ADD=1;
	public static int OP_MULT=2;
	public static int OP_DIV=3;
	public static int OP_SUB=4;
	public static int OP_MEAN=5;
	public static void main(String[]args) {
		System.out.println("File compiled ");


	}

	public final static String slash=File.separator;
	public static final int ERROR_VALUE=-1;
	public static final int COORD_OF_MAX_IN_TWO_LAST_SLICES=1;
	public static final int COORD_OF_MAX_ALONG_Z=2;

	public static  AcquisitionType stringToAcquisitionType(String str){
		switch(str) {
		case "MRI_SE_SEQ": return AcquisitionType.MRI_SE_SEQ;
		case "MRI_T1_SEQ": return AcquisitionType.MRI_T1_SEQ;
		case "MRI_T2_SEQ": return AcquisitionType.MRI_T2_SEQ;
		case "MRI_GE3D": return AcquisitionType.MRI_GE3D;
		case "MRI_DIFF_SEQ": return AcquisitionType.MRI_DIFF_SEQ;
		case "MRI_FLIPFLOP_SEQ": return AcquisitionType.MRI_FLIPFLOP_SEQ;
		case "RX": return AcquisitionType.RX;
		case "HISTOLOGY": return AcquisitionType.HISTOLOGY;
		case "PHOTOGRAPH": return AcquisitionType.PHOTOGRAPH;
		case "TERAHERTZ": return AcquisitionType.TERAHERTZ;
		}
		return null;
	}
	
	public static VineType stringToVineType(String str){
		switch(str) {
		case "GRAFTED_VINE": return VineType.GRAFTED_VINE;
		
		case "VINE": return VineType.VINE;
		case "CUTTING": return VineType.CUTTING;
		}
		return null;
	}
	
	public enum ComputingType{
		COMPUTE_ALL,
		EVERYTHING_UNTIL_MAPS,
		EVERYTHING_AFTER_MAPS
	}
	
	public enum AcquisitionType{
		MRI_CLINICAL,
		MRI_SE_SEQ,
		MRI_T1_SEQ,
		MRI_T2_SEQ,
		MRI_DIFF_SEQ,
		MRI_FLIPFLOP_SEQ,
		RX,
		HISTOLOGY,
		PHOTOGRAPH,
		TERAHERTZ,
		MRI_GE3D
	}

	public enum SupervisionLevel{
		AUTONOMOUS,
		GET_INFORMED,
		ASK_FOR_ALL
	}	

	public enum Capillary{
		HAS_CAPILLARY,
		HAS_NO_CAPILLARY
	}

	public static Date getDateFromString(String datStr) {
		Date date=null;
		try {
			date = new SimpleDateFormat("yyyyMMdd").parse(datStr);
		} catch (ParseException e) {
			return new Date(0);
		}  
		return date;
	}

	
	public static double[]getColumnOfTab(double[][]tab,int column){
		double[]ret=new double[tab.length];
		for(int i=0;i<tab.length;i++)ret[i]=tab[i][column];
		return ret;
	}
	
	public static double[][]transposeTab(double[][]tab){
		double[][]ret=new double[tab[0].length][tab.length];
		for(int i=0;i<tab.length;i++)for(int j=0;j<tab[0].length;j++)ret[j][i]=tab[i][j];
		return ret;
	}

	public static int[][] readIntArrayFromFile(String file,int nbDimsPerLine) {
		File fParam=new File(file);
		int nData;
		int[][]vals;
		String[]strFile=null;
		String[]strLine=null;
		try {
			 String str= Files.lines(Paths.get(fParam.getAbsolutePath()) ).collect(Collectors.joining("\n"));
			 strFile=str.split("\n");
        } catch (IOException ex) {        ex.printStackTrace();   }
		nData=Integer.parseInt(strFile[0]);
		vals=new int[nData][nbDimsPerLine];
		for(int i=1;i<=nData ; i++) {
			strLine=strFile[i].split(" ");
			for(int j=0;j<nbDimsPerLine;j++) {
				vals[i-1][j]=Integer.parseInt(strLine[j]);			
			}
		}
		return vals;
	}

	public static void setLabelOnAllSlices(ImagePlus img,String label) {
		for(int i=0;i<img.getStack().getSize();i++)img.getStack().setSliceLabel(label,i+1);
	}
	
	public static Point3d toRealSpace(Point3d p,double[]voxs) {
		return new Point3d(p.x*voxs[0],p.y*voxs[1],p.z*voxs[2]);
	}
	public static Point3d toImageSpace(Point3d p,double[]voxs) {
		return new Point3d(p.x/voxs[0],p.y/voxs[1],p.z/voxs[2]);
	}

	
	public static String getSystemName(){
		String os=System.getProperty("os.name").toLowerCase();
		if(os.indexOf("win") >= 0)return "Windows system";
		if(os.indexOf("mac") >= 0)return "Mac iOs system";
		if(os.indexOf("nux") >= 0)return "Linux system";
		return "System";
	}

	public static boolean isWindowsOS(){
		String os=System.getProperty("os.name").toLowerCase();
		return (os.indexOf("win") >= 0);
	}

	
	public static String getSystemNeededMemory(){
		String os=System.getProperty("os.name").toLowerCase();
		if(os.indexOf("win") >= 0)return "3000 MB";
		if(os.indexOf("mac") >= 0)return "3000 MB";
		if(os.indexOf("nux") >= 0)return "3000 MB";
		return "System";
	}
	
	public static double[][] readDoubleArrayFromFile(String file,int nbDimsPerLine) {
		File fParam=new File(file);
		int nData;
		double[][]vals;
		String[]strFile=null;
		String[]strLine=null;
		try {
			 String str= Files.lines(Paths.get(fParam.getAbsolutePath()) ).collect(Collectors.joining("\n"));
			 strFile=str.split("\n");
        } catch (IOException ex) {        ex.printStackTrace();   }
		nData=Integer.parseInt(strFile[0]);
		vals=new double[nData][nbDimsPerLine];
		for(int i=1;i<=nData ; i++) {
			strLine=strFile[i].split(" ");
			for(int j=0;j<nbDimsPerLine;j++) {
				vals[i-1][j]=Double.parseDouble(strLine[j]);
				
			}
		}
		return vals;
	}
		
	
	
	
	
	
	
	public static ImagePlus computeGeodesicDistanceMap(ImagePlus imgSeg,int labelSeed,int firstLabelIncludedInExtensionArea,int firstLabelExcludedFromExtensionArea,int referenceDimForAnisotropyReduction) {
		ImagePlus imgSegIso=VitimageUtils.imageCopy(imgSeg);
		double[]voxS=VitimageUtils.getVoxelSizes(imgSeg);
		double[]factorS=new double[3];
		boolean anisotropic=false;
		if( (   (Math.abs((voxS[0]-voxS[1])/(0.5*voxS[0]+0.5*voxS[1]) ) >0.05) || (Math.abs((voxS[0]-voxS[2])/(0.5*voxS[0]+0.5*voxS[2]) ) >0.05) ) )anisotropic=true;
		if(anisotropic) {
			System.out.println("Preprocessing : reducing anisotropy");
			double globFactor=0;
			factorS[referenceDimForAnisotropyReduction]=1;
			globFactor=voxS[referenceDimForAnisotropyReduction];
			factorS[(referenceDimForAnisotropyReduction+1)%3]=voxS[(referenceDimForAnisotropyReduction+1)%3]/voxS[referenceDimForAnisotropyReduction];
			factorS[(referenceDimForAnisotropyReduction+2)%3]=voxS[(referenceDimForAnisotropyReduction+2)%3]/voxS[referenceDimForAnisotropyReduction];			
			imgSegIso=VitimageUtils.subXYZPerso(imgSegIso,factorS,false,0);
		}
				
		float[] floatWeights = new float[] {1000,1414,1732};
		ImagePlus maskImage=VitimageUtils.thresholdByteImage(imgSegIso,firstLabelIncludedInExtensionArea,firstLabelExcludedFromExtensionArea);
		ImagePlus seedImage=VitimageUtils.thresholdByteImage(imgSegIso, labelSeed, labelSeed+1);
		ImagePlus distance=new ImagePlus(
				"geodistance",new GeodesicDistanceTransform3DFloat(
						floatWeights,true).geodesicDistanceMap(seedImage.getStack(), maskImage.getStack())
				);		
		if( anisotropic ){
			System.out.println("postprocessing : giving anisotropy");
			double[]factorSinv=TransformUtils.invertVector(factorS);
			distance=VitimageUtils.subXYZPerso(distance,factorSinv,true,-1);
		}
		distance=VitimageUtils.noNanInFloat(distance,(float) -1);
		return distance;
	}
	
	
	
	
	
	
	
	
	public static int getNbCores() {
		return	Runtime.getRuntime().availableProcessors();
	}
	
	public static void writeStringInFile(String text,String file) {
		if(file ==null)return;
		try {
			Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
			out.write(text);
			out.close();
		} catch (Exception e) {IJ.error("Unable to write data to file: "+file+"error: "+e);}			
	}
	
	public static String readStringFromFile(String file) {
		String str=null;
		try {
			str= Files.lines(Paths.get(new File(file).getAbsolutePath()) ).collect(Collectors.joining("\n"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return str;
	}
	
	public static void writeIntInFile(String file,int a) {
		writeStringInFile(""+a,file);
	}
	
	public static int readIntFromFile(String file) {
		return Integer.parseInt(readStringFromFile(file));
	}
	
	public static void writeDoubleInFile(String file,double a) {
		writeStringInFile(""+a,file);
	}
	
	public static double readDoubleFromFile(String file) {
		return Double.parseDouble(readStringFromFile(file));
	}
	

	
	
	public static void writeDoubleArrayInFile(double [][]tab,String file) {
		int nData=tab.length;
		if(nData<1)return;
		int nDims=tab[0].length;
		try {
			Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
			out.write(""+tab.length+"\n");
			for(int i=0;i<nData;i++) {
				for(int j=0;j<nDims-1;j++) {
					out.write(""+tab[i][j]+" ");
				}
				out.write(""+tab[i][nDims-1]+"\n");
			}
			out.close();
		} catch (Exception e) {IJ.error("Unable to write data to file: "+file+"error: "+e);}	
	}
	

	
	public static void writeIntArrayInFile(int [][]tab,String file) {
		int nData=tab.length;
		if(nData<1)return;
		int nDims=tab[0].length;
		try {
			Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
			out.write(""+tab.length+"\n");
			for(int i=0;i<nData;i++) {
				for(int j=0;j<nDims-1;j++) {
					out.write(""+tab[i][j]+" ");
				}
				out.write(""+tab[i][nDims-1]+"\n");
			}
			out.close();
		} catch (Exception e) {IJ.error("Unable to write data to file: "+file+"error: "+e);}	
	}
	
	public static void writeIntArray1DInFile(int []tab,String file) {
		int nData=tab.length;
		if(nData<1)return;
		try {
			Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
			out.write(""+nData+"\n");
			for(int i=0;i<nData;i++) out.write(tab[i]+"\n");
			out.close();
		} catch (Exception e) {IJ.error("Unable to write data to file: "+file+"error: "+e);}	
	}
	
	public static int[] readIntArray1DFromFile(String file) {
		File fParam=new File(file);
		int nData;
		int[]vals;
		String[]strFile=null;
		String[]strLine=null;
		try {
			 String str= Files.lines(Paths.get(fParam.getAbsolutePath()) ).collect(Collectors.joining("\n"));
			 strFile=str.split("\n");
        } catch (IOException ex) {        ex.printStackTrace();   }
		nData=Integer.parseInt(strFile[0]);
		vals=new int[nData];
		for(int i=1;i<=nData ; i++) {
			vals[i-1]=Integer.parseInt(strFile[i]);			
		}
		return vals;
	}

	
	public static void writeDoubleArray1DInFile(double []tab,String file) {
		int nData=tab.length;
		if(nData<1)return;
		try {
			Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
			out.write(""+nData+"\n");
			for(int i=0;i<nData;i++) out.write(tab[i]+"\n");
			out.close();
		} catch (Exception e) {IJ.error("Unable to write data to file: "+file+"error: "+e);}	
	}
	
	public static double[] readDoubleArray1DFromFile(String file) {
		File fParam=new File(file);
		int nData;
		double[]vals;
		String[]strFile=null;
		String[]strLine=null;
		try {
			 String str= Files.lines(Paths.get(fParam.getAbsolutePath()) ).collect(Collectors.joining("\n"));
			 strFile=str.split("\n");
        } catch (IOException ex) {        ex.printStackTrace();   }
		nData=Integer.parseInt(strFile[0]);
		vals=new double[nData];
		for(int i=1;i<=nData ; i++) {
			vals[i-1]=Double.parseDouble(strFile[i]);			
		}
		return vals;
	}

	
	public static void writePoint3dArrayInFile(Point3d[]tab,String file) {
		writeDoubleArrayInFile(VitimageUtils.convertPoint3dArrayToDoubleArray(tab),file);
	}
	
	public static Point3d[] readPoint3dArrayInFile(String file) {
		return VitimageUtils.convertDoubleArrayToPoint3dArray(VitimageUtils.readDoubleArrayFromFile(file,3));
	}

	
/*	public static Object[] getCorrespondanceListAsImagePlus(ImagePlus imgRef,Point3d[][]tabpt,double[]curVoxSize,int sliceInt,int blockStrideX,int blockStrideY,int blockStrideZ,int blockSizeX,int blockSizeY,int blockSizeZ,boolean vectors) {
		ArrayList<double[][]>tabCorr=new ArrayList();
		for (int coup=0;coup<tabpt[0].length;coup++) {
			tabCorr.add(  new double[][] {   {tabpt[0][coup].x,tabpt[0][coup].y,tabpt[0][coup].z} , {tabpt[1][coup].x,tabpt[1][coup].y,tabpt[1][coup].z} ,{1,1} });
		}
		return getCorrespondanceListAsImagePlus(imgRef,tabCorr,curVoxSize,sliceInt,blockStrideX,blockStrideY,blockStrideZ,blockSizeX,blockSizeY,blockSizeZ,vectors);
	}
	*/
	
	public static Object[] getCorrespondanceListAsImagePlus(ImagePlus imgRef,ArrayList<double[][]>tabCorr,double[]curVoxSize,int sliceInt2,int blockStrideX,int blockStrideY,int blockStrideZ,int blockHalfSizeX,int blockHalfSizeY,int blockHalfSizeZ,boolean vectors) {
		int [] dims=VitimageUtils.getDimensions(imgRef);		
		int sliceInt=(sliceInt2<0 ? dims[2]/2 : sliceInt2);
		int sliceIntCorr=0;
		int distMin=1000000;
		ImagePlus ret=IJ.createImage("corr", dims[0], dims[1], dims[2], 32);
		VitimageUtils.adjustImageCalibration(ret, imgRef);
		double [] voxS=VitimageUtils.getVoxelSizes(imgRef);
		float[][]dataRet=new float[dims[2]][];
		for(int z=0;z<dims[2];z++) {
			dataRet[z]=(float[])ret.getStack().getProcessor(z+1).getPixels();
		}

		
		if(!vectors) {
			int dx=Math.min(blockStrideX/2-1, blockHalfSizeX);
			int dy=Math.min(blockStrideY/2-1, blockHalfSizeY);
			int dz=Math.min(blockStrideZ/2-1, blockHalfSizeZ);
			for(int cor=0;cor<tabCorr.size();cor++) {
				int x=(int)Math.round(tabCorr.get(cor)[0][0]/voxS[0]*curVoxSize[0]  );
				int y=(int)Math.round(tabCorr.get(cor)[0][1]/voxS[1]*curVoxSize[1]  );
				int z=(int)Math.round(tabCorr.get(cor)[0][2]/voxS[2]*curVoxSize[2]  );
				if(Math.abs(sliceInt-z) < distMin) {
					distMin=Math.abs(sliceInt-z);
					sliceIntCorr=z;
				}
				float score=(float)tabCorr.get(cor)[2][0];
				int x0=Math.max(0, x-dx);
				int y0=Math.max(0, y-dy);
				int z0=Math.max(0, z-dz);
				int xf=Math.min(dims[0]-1, x+dx);
				int yf=Math.min(dims[1]-1, y+dy);
				int zf=Math.min(dims[2]-1, z+dz);
				for(int xx=x0;xx<=xf;xx++) {
					for(int yy=y0;yy<=yf;yy++) {
						for(int zz=z0;zz<=zf;zz++) {
							dataRet[zz][dims[0]*yy+xx]=score;
						}
					}
				}
			}
		}
		else{
			
			for(int cor=0;cor<tabCorr.size();cor++) {
				int x0=(int)Math.round(tabCorr.get(cor)[0][0]/voxS[0]*curVoxSize[0]  );
				int y0=(int)Math.round(tabCorr.get(cor)[0][1]/voxS[1]*curVoxSize[1]  );
				int z0=(int)Math.round(tabCorr.get(cor)[0][2]/voxS[2]*curVoxSize[2]  );
				int xf=(int)Math.round(tabCorr.get(cor)[1][0]/voxS[0]*curVoxSize[0]  );
				int yf=(int)Math.round(tabCorr.get(cor)[1][1]/voxS[1]*curVoxSize[1]  );
				int zf=(int)Math.round(tabCorr.get(cor)[1][2]/voxS[2]*curVoxSize[2]  );
				double dx=(xf-x0)/5;
				double dy=(yf-y0)/5;
				double dz=(zf-z0)/5;
				float score=(float)tabCorr.get(cor)[2][0];
				//Bras fleche suivant XY
				for(int dt=0;dt<=15;dt++) {
					int zz=z0;
					int xx=x0+(int)Math.round(dx*dt);
					int yy=y0+(int)Math.round(dy*dt);
					dataRet[zz][dims[0]*yy+xx]=score;
				}
				//Bras fleche suivant XZ
				for(int dt=0;dt<=15;dt++) {
					int yy=y0;
					int xx=x0+(int)Math.round(dx*dt);
					int zz=z0+(int)Math.round(dz*dt);
					dataRet[zz][dims[0]*yy+xx]=score;
				}
				//Bras fleche suivant YZ
				for(int dt=0;dt<=15;dt++) {
					int xx=x0;
					int zz=z0+(int)Math.round(dz*dt);
					int yy=y0+(int)Math.round(dy*dt);
					dataRet[zz][dims[0]*yy+xx]=score;
				}
				//Base fleche
				for(int ddz=-1;ddz<=1;ddz++)for(int ddx=-1;ddx<=1;ddx++)for(int ddy=-1;ddy<=1;ddy++)dataRet[z0+ddz][dims[0]*(y0+ddy)+x0+ddx]=score;
			}
		}

		return new Object[] {ret,sliceIntCorr};		
	}
	
	
	
	
	
	
	public static String writableArray(double[]array) {
		String ret="";
		for(int i=0;i<array.length-1;i++)ret+=array[i]+" ";
		ret+=array[array.length-1];
		return ret;
	}

	public static String writableArray(int[]array) {
		String ret="";
		for(int i=0;i<array.length-1;i++)ret+=array[i]+" ";
		ret+=array[array.length-1];
		return ret;
	}

	
	public static ImagePlus[]yoloGradients(ImagePlus imgRef,int radius,double interMin,double interMax,boolean debug){
		ImagePlus imgFloat=VitimageUtils.noNanInFloat(imgRef,0);
		ImagePlus img[]=new ImagePlus[3];
		img[0]=VitimageUtils.imageCopy(imgFloat);
		img[0]=VitimageUtils.makeOperationOnOneImage(img[0],2,0,true);
		img[1]=VitimageUtils.imageCopy(img[0]);
		img[2]=VitimageUtils.imageCopy(img[0]);
		
		float[][] in=new float[imgFloat.getStackSize()][];
		float[][][] out=new float[3][imgFloat.getStackSize()][];
		int X=imgFloat.getWidth();
		int Y=imgFloat.getHeight();
		int Z=imgFloat.getStackSize();
		double val;
		int countx,countX,county,countY,countz,countZ;
		float sumX,sumx,sumY,sumy,sumZ,sumz;
		for(int z=0;z<Z;z++) {
			in[z]=(float []) imgFloat.getStack().getProcessor(z+1).getPixels();
			out[0][z]=(float []) img[0].getStack().getProcessor(z+1).getPixels();
			out[1][z]=(float []) img[1].getStack().getProcessor(z+1).getPixels();
			out[2][z]=(float []) img[2].getStack().getProcessor(z+1).getPixels();
		}
		for(int z=radius+1;z<Z-radius-1;z++) {
			if(z%50==0)System.out.print(" "+z);
			for(int x=radius+1;x<X-radius-1;x++) {
				for(int y=radius+1;y<Y-radius-1;y++) {
					sumX=sumx=sumy=sumY=sumz=sumZ=countx=countX=county=countY=countz=countZ=0;
					for(int di=-radius;di<radius+1;di++) {
						for(int dj=-radius;dj<radius+1;dj++) {
							val=in[z+di][(y+dj)*X+(x-1)];if(val>=interMin && val<=interMax) {sumx+=val;countx++;}
							val=in[z+di][(y+dj)*X+(x+1)];if(val>=interMin && val<=interMax) {sumX+=val;countX++;}
							val=in[z+di][(y-1)*X+(x+dj)];if(val>=interMin && val<=interMax) {sumy+=val;county++;}
							val=in[z+di][(y+1)*X+(x+dj)];if(val>=interMin && val<=interMax) {sumY+=val;countY++;}
							val=in[z-1][(y+dj)*X+(x+di)];if(val>=interMin && val<=interMax) {sumz+=val;countz++;}
							val=in[z+1][(y+dj)*X+(x+di)];if(val>=interMin && val<=interMax) {sumZ+=val;countZ++;}
						}
					}
					out[0][z][y*X+x]=(countx==0||countX==0) ? 0 : (float)((sumX/countX-sumx/countx)/2.0);
					out[1][z][y*X+x]=(county==0||countY==0) ? 0 : (float)((sumY/countY-sumy/county)/2.0);
					out[2][z][y*X+x]=(countz==0||countZ==0) ? 0 : (float)((sumZ/countZ-sumz/countz)/2.0);
				}			 
			}
		}
		System.out.println();
		if(debug)img[0].show();
		img[0].setTitle("GradX yolo");
		img[0].setDisplayRange(-2, 2);
		if(debug)img[1].show();
		img[1].setTitle("GradY yolo");
		img[1].setDisplayRange(-2, 2);
		if(debug)img[2].show();
		img[2].setTitle("GradZ yolo");
		img[2].setDisplayRange(-2, 2);

		return img;		
	}
	
	public static ImagePlus noNanInFloat(ImagePlus imgRef,float replacementValue) {
		ImagePlus img=VitimageUtils.imageCopy(imgRef);
		float[][] in=new float[img.getStackSize()][];
		int X=img.getWidth();
		int Y=img.getHeight();
		int Z=img.getStackSize();
		for(int z=0;z<Z;z++) {
			in[z]=(float []) img.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<X;x++) {
				for(int y=0;y<Y;y++) {
					if(Float.isNaN((float)(in[z][y*X+x])) )in[z][y*X+x]=replacementValue; 
					if(Float.isInfinite((float)(in[z][y*X+x])) )in[z][y*X+x]=replacementValue; 
				}			 
			}
		}
		return img;
	}
	
	public static ImagePlus thresholdFloatImage(ImagePlus img,double thresholdMin, double thresholdMax) {
		ImagePlus ret=new Duplicator().run(img);
		VitimageUtils.adjustImageCalibration(ret,img);
		IJ.run(ret,"8-bit","");
		float[][] in=new float[img.getStackSize()][];
		byte[][] out=new byte[ret.getStackSize()][];
		int X=img.getWidth();
		int Y=img.getHeight();
		int Z=img.getStackSize();
		for(int z=0;z<Z;z++) {
			in[z]=(float []) img.getStack().getProcessor(z+1).getPixels();
			out[z]=(byte []) ret.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<X;x++) {
				for(int y=0;y<Y;y++) {
					out[z][y*X+x]=(  in[z][y*X+x] >= thresholdMin ? (        in[z][y*X+x] < thresholdMax  ?   ((byte) (255 & 0xff)) : ((byte) (0 & 0xff)) ) : ((byte) (0 & 0xff)) );
				}			 
			}
		}
		return ret;

	}
	
	
	public static ImagePlus compositeGridByte(ImagePlus img1,ImagePlus img2,int dx,int dy,int dz,String title) {
		ImagePlus retR=VitimageUtils.imageCopy(img1);
		ImagePlus retG=VitimageUtils.imageCopy(img1);
		byte[][] in1=new byte[img1.getStackSize()][];
		byte[][] in2=new byte[img2.getStackSize()][];
		byte[][] outR=new byte[img1.getStackSize()][];
		byte[][] outG=new byte[img1.getStackSize()][];
		int X=img1.getWidth();
		int Y=img1.getHeight();
		int Z=img1.getStackSize();
		int val;
		for(int z=0;z<Z;z++) {
			in1[z]=(byte []) img1.getStack().getProcessor(z+1).getPixels();
			in2[z]=(byte []) img2.getStack().getProcessor(z+1).getPixels();
			outR[z]=(byte []) retR.getStack().getProcessor(z+1).getPixels();
			outG[z]=(byte []) retG.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<X;x++) {
				for(int y=0;y<Y;y++) {
					val=(z/dz+y/dy+x/dx)%3;
					outR[z][y*X+x]=val>0 ? in1[z][y*X+x] : (byte)(0 & 0xff);
					outG[z][y*X+x]=val<2 ? in2[z][y*X+x] : (byte)(0 & 0xff);
				}			 
			}
		}
		return VitimageUtils.compositeOf(retR, retG,title);
	}

	
	
	
	public static ImagePlus morpho(ImagePlus img,int op_1DIL_2ERO,double radius) {
		ImagePlus imgOut=VitimageUtils.imageCopy(img);
		Strel3D str=inra.ijpb.morphology.strel.BallStrel.fromRadius(radius);
		if(op_1DIL_2ERO==1)return new ImagePlus("",Morphology.dilation(imgOut.getImageStack(),str));
		if(op_1DIL_2ERO==2)return new ImagePlus("",Morphology.erosion(imgOut.getImageStack(),str));
		else return null;
	}
	
	
	
	
	public static ImagePlus maskForRemovingThinStructuresInIsotropicImage(ImagePlus imgIn,int threshold,int radiusVoxel) {
		ImagePlus mask=VitimageUtils.thresholdShortImage(imgIn, threshold, 10E8);
		Strel3D strPti=inra.ijpb.morphology.strel.CuboidStrel.fromRadiusList(1,1,1);
		Strel3D str=inra.ijpb.morphology.strel.CuboidStrel.fromRadiusList(radiusVoxel,radiusVoxel, radiusVoxel);
		mask =new ImagePlus("",Morphology.dilation(mask.getImageStack(),strPti));
		mask =new ImagePlus("",Morphology.closing(mask.getImageStack(),str));
		mask =new ImagePlus("",Morphology.opening(mask.getImageStack(),str));
		VitimageUtils.adjustImageCalibration(mask, imgIn);
		return mask;		
	}

	public static ImagePlus nullImage(ImagePlus in) {
		return makeOperationOnOneImage(in,2,0, true);
	}

	
	public static ImagePlus makeOperationOnOneImage(ImagePlus in,int op_1add_2mult_3div_4sub,double val,boolean copyBefore) {
		ImagePlus img=null;
		if(copyBefore) {
			img=new Duplicator().run(in);
			VitimageUtils.adjustImageCalibration(img, in);
		}
		else img=in;
		switch (op_1add_2mult_3div_4sub) {
		case OP_ADD :IJ.run(img, "Add...", "value="+val+" stack");break;
		case OP_MULT :IJ.run(img, "Multiply...", "value="+val+" stack");break;
		case OP_DIV :IJ.run(img, "Divide...", "value="+val+" stack");break;
		case OP_SUB :IJ.run(img, "Substract...", "value="+val+" stack");break;
		}
		return img;
	}
	
	public static ImagePlus makeOperationBetweenTwoImages(ImagePlus in1,ImagePlus in2,int op_1add_2mult_3div_4sub,boolean make32bitResult) {
		ImagePlus img=null;
		img=new Duplicator().run(in1);
		VitimageUtils.adjustImageCalibration(img, in1);
		ImageCalculator ic = new ImageCalculator();
		ImagePlus res=null;
		switch (op_1add_2mult_3div_4sub) {
		case OP_ADD :res=ic.run("Add "+(make32bitResult ? "32-bit" : "")+" create stack", img, in2);break;
		case OP_MULT :res=ic.run("Multiply "+(make32bitResult ? "32-bit" : "")+" create stack", img, in2);break;
		case OP_DIV :res=ic.run("Divide "+(make32bitResult ? "32-bit" : "")+" create stack", img, in2);break;
		case OP_SUB :res=ic.run("Substract "+(make32bitResult ? "32-bit" : "")+" create stack", img, in2);break;
		case OP_MEAN :res=ic.run("Average "+(make32bitResult ? "32-bit" : "")+" create stack", img, in2);break;
		}
		res.hide();
		return res;
	}
	
	
	public static ImagePlus binaryOperationBetweenTwoImages(ImagePlus in1,ImagePlus in2,int op_1OR_2AND_3Pouet_4SUB) {
		ImagePlus img=null;
		img=new Duplicator().run(in1);
		VitimageUtils.adjustImageCalibration(img, in1);
		ImageCalculator ic = new ImageCalculator();
		ImagePlus res=null;
		switch (op_1OR_2AND_3Pouet_4SUB) {
		case OP_ADD :res=ic.run("OR "+" create stack", img, in2);break;
		case OP_MULT :res=ic.run("AND "+" create stack", img, in2);break;
		case OP_DIV :return null;
		case OP_SUB :res=ic.run("DIFF "+" create stack", img, in2);break;
		}
		res.hide();
		return res;
	}


	public static ImagePlus switchAxis(ImagePlus img,int switch_0XY_1XZ_2YZ) {
		ImagePlus ret=null;
		int X=img.getWidth();
		int Y=img.getHeight();
		int Z=img.getStackSize();
		int X2=img.getWidth();
		int Y2=img.getHeight();
		int Z2=img.getStackSize();
		double[]voxOut=VitimageUtils.getVoxelSizes(img);
		double temp;
		int tem;
		if(switch_0XY_1XZ_2YZ==0) {
			ret=ij.gui.NewImage.createImage("Mask",Y,X,Z,img.getBitDepth(),ij.gui.NewImage.FILL_BLACK);	
			temp=voxOut[0];voxOut[0]=voxOut[1];voxOut[1]=temp; 
			tem=X2; X2=Y2;Y2=tem;
			VitimageUtils.adjustImageCalibration(ret, voxOut,"mm");
		}	
		if(switch_0XY_1XZ_2YZ==1) {
			ret=ij.gui.NewImage.createImage("Mask",Z,Y,X,img.getBitDepth(),ij.gui.NewImage.FILL_BLACK);	
			temp=voxOut[0];voxOut[0]=voxOut[2];voxOut[2]=temp; 
			tem=X2;X2=Z2;Z2=tem;
			VitimageUtils.adjustImageCalibration(ret, voxOut,"mm");
		}	
		if(switch_0XY_1XZ_2YZ==2) {
			ret=ij.gui.NewImage.createImage("Mask",X,Z,Y,img.getBitDepth(),ij.gui.NewImage.FILL_BLACK);
			temp=voxOut[1];voxOut[1]=voxOut[2];voxOut[2]=temp;
			tem=Y2;Y2=Z2;Z2=tem;
			VitimageUtils.adjustImageCalibration(ret, voxOut,"mm");
		}	

		
		if(img.getType()==ImagePlus.GRAY8) {
			byte[][] in=new byte[Z][];
			byte[][] out=new byte[Z2][];
			for(int z=0;z<Z;z++)in[z]=(byte []) img.getStack().getProcessor(z+1).getPixels();
			for(int z=0;z<Z2;z++)out[z]=(byte []) ret.getStack().getProcessor(z+1).getPixels();
			
			for(int z=0;z<Z;z++) {
				if(z%10==0)System.out.print("  z="+z+"/"+Z);
				for(int x=0;x<X;x++) {
					for(int y=0;y<Y;y++) {
						if(switch_0XY_1XZ_2YZ==0)out[z][x*Y+y]=in[z][y*X+x];
						if(switch_0XY_1XZ_2YZ==1)out[x][y*Z+z]=in[z][y*X+x];
						if(switch_0XY_1XZ_2YZ==2)out[y][z*X+x]=in[z][y*X+x];
					}			 
				}
			}
			System.out.println();
			return ret;
		}
		if(img.getType()==ImagePlus.GRAY16) {
			short[][] in=new short[Z][];
			short[][] out=new short[Z2][];
			for(int z=0;z<Z;z++)in[z]=(short []) img.getStack().getProcessor(z+1).getPixels();
			for(int z=0;z<Z2;z++)out[z]=(short []) ret.getStack().getProcessor(z+1).getPixels();
			for(int z=0;z<Z;z++) {
				if(z%10==0)System.out.print("  z="+z+"/"+Z);
				for(int x=0;x<X;x++) {
					for(int y=0;y<Y;y++) {
						if(switch_0XY_1XZ_2YZ==0)out[z][x*Y+y]=in[z][y*X+x];
						if(switch_0XY_1XZ_2YZ==1)out[x][y*Z+z]=in[z][y*X+x];
						if(switch_0XY_1XZ_2YZ==2)out[y][z*X+x]=in[z][y*X+x];
					}			 
				}
			}
			System.out.println();
			return ret;
		}
		if(img.getType()==ImagePlus.GRAY32) {
			float[][] in=new float[Z][];
			float[][] out=new float[Z2][];
			for(int z=0;z<Z;z++)in[z]=(float []) img.getStack().getProcessor(z+1).getPixels();
			for(int z=0;z<Z2;z++)out[z]=(float []) ret.getStack().getProcessor(z+1).getPixels();
			for(int z=0;z<Z;z++) {
				if(z%10==0)System.out.print("  z="+z+"/"+Z);
				for(int x=0;x<X;x++) {
					for(int y=0;y<Y;y++) {
						if(switch_0XY_1XZ_2YZ==0)out[z][x*Y+y]=in[z][y*X+x];
						if(switch_0XY_1XZ_2YZ==1)out[x][y*Z+z]=in[z][y*X+x];
						if(switch_0XY_1XZ_2YZ==2)out[y][z*X+x]=in[z][y*X+x];
					}			 
				}
			}
			System.out.println();
			return ret;
		}
		else {
			System.out.println("Switch axis : unsupported format");return null;
		}
	}
	
	public static double[]getImageCenter(ImagePlus ref,boolean giveCoordsInRealSpace){
		int[]dims=VitimageUtils.getDimensions(ref);
		double[]voxs=VitimageUtils.getVoxelSizes(ref);
		double[]ret=new double[3];
		for(int dim=0;dim<3;dim++)ret[dim]=(dims[dim]-1)/2.0*(giveCoordsInRealSpace ? voxs[dim] : 1);
		return ret;
	}
	
	
	public static int []getSliceRootStocks(){
		return new int[] {200+86 , 200+76 , 200+174 ,          200+65 , 200+97 , 200+108 ,        200+71  , 200+74 ,  200+68 ,       200+59 , 200+42  , 200+118}; 
	}

	public static int []getSliceRootStocksDown(){
		return new int[] {200+86 , 200+76 , 200+174 ,          200+65 , 200+97 , 200+108 ,        200+71  , 200+74 ,  200+68 ,       200+59 , 200+42  , 200+118}; 
	}
	
	

	
	
	public static ImagePlus thresholdShortImage(ImagePlus img,double thresholdMin, double thresholdMax) {
		ImagePlus ret=new Duplicator().run(img);
		VitimageUtils.adjustImageCalibration(ret,img);
		IJ.run(ret,"8-bit","");
		short[][] in=new short[img.getStackSize()][];
		byte[][] out=new byte[ret.getStackSize()][];
		int X=img.getWidth();
		int Y=img.getHeight();
		int Z=img.getStackSize();
		for(int z=0;z<Z;z++) {
			in[z]=(short []) img.getStack().getProcessor(z+1).getPixels();
			out[z]=(byte []) ret.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<X;x++) {
				for(int y=0;y<Y;y++) {
					int val=(int)(in[z][y*X+x] & 0xffff);
					out[z][y*X+x]=(  val >= thresholdMin ? (        val < thresholdMax  ?   ((byte) (255 & 0xff)) : ((byte) (0 & 0xff)) ) : ((byte) (0 & 0xff)) );
				}			 
			}
		}
		return ret;

	}
	
	
	
	public static Point3d dou(Point3d p) {
		return new Point3d(VitimageUtils.dou(p.x),VitimageUtils.dou(p.y),VitimageUtils.dou(p.z));
	}
	
	
	public static Point3d[] getMinMaxPoints(Point3d[]tab) {
		Point3d[]tabRet=new Point3d[] {new Point3d(100000,10000000,1000000),new Point3d(-10000000,-10000000,-10000000)};
		for(Point3d p : tab) {
			if(p.x < tabRet[0].x)tabRet[0].x=p.x;
			if(p.y < tabRet[0].y)tabRet[0].y=p.y;
			if(p.z < tabRet[0].z)tabRet[0].z=p.z;
			if(p.x > tabRet[1].x)tabRet[1].x=p.x;
			if(p.y > tabRet[1].y)tabRet[1].y=p.y;
			if(p.z > tabRet[1].z)tabRet[1].z=p.z;
		}	
		return tabRet;
	}
	
	public static int[][]countVolumeBySlices(ImagePlus img,int nMax) {
		byte[] in;		
		int X=img.getWidth();
		int Y=img.getHeight();
		int Z=img.getStackSize();
		int[][]tabRet=new int[nMax][Z];
		for(int z=0;z<Z;z++) {
			in=(byte []) img.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<X;x++) {
				for(int y=0;y<Y;y++) {
					int val=(int)(in[y*X+x] & 0xff);
					if(val>0 && val<=nMax)
					tabRet[val-1][z]++;
				}			 
			}
		}
		return tabRet;
	}
	
	public static void printVolumeOfObject(ImagePlus img) {
		byte[] in;		
		int X=img.getWidth();
		int Y=img.getHeight();
		int Z=img.getStackSize();
		double []voxS=VitimageUtils.getVoxelSizes(img);
		double voxV=voxS[0]*voxS[1]*voxS[2];
		int count=0;
		for(int z=0;z<Z;z++) {
			in=(byte []) img.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<X;x++) {
				for(int y=0;y<Y;y++) {
					int val=(int)(in[y*X+x] & 0xff);
					if(val>0)count++;
				}			 
			}
		}
		System.out.println("Nb voxels="+count+" ... volume (units^3)="+VitimageUtils.dou(count*voxV));
	}
	
	
	public static int[][]countVolumeByDistance(ImagePlus seg,ImagePlus distance,int nMax,double valMin,double valMax,double valStep) {
		byte[] valSeg;		
		float[] valDist;
		int nbCat=(int)(Math.ceil((valMax-valMin)/(1.0*valStep)));
		int[][]tabRet=new int[nMax][nbCat]; 
		int X=seg.getWidth();
		int Y=seg.getHeight();
		int Z=seg.getStackSize();
		for(int z=0;z<Z;z++) {
			valSeg=(byte []) seg.getStack().getProcessor(z+1).getPixels();
			valDist=(float []) distance.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<X;x++) {
				for(int y=0;y<Y;y++) {
					int cat=(int)(valSeg[y*X+x] & 0xff);
					double dis=(double)(valDist[y*X+x]);
					if(cat>0 && cat<=nMax && dis>=valMin && dis <valMax) tabRet[cat-1][(int)Math.floor((dis-valMin)/valStep)]++;
				}			 
			}
		}
		return tabRet;
	}
	
	//Return an image of the pixels with label zoneIn with at least a neighbour with label zoneOut
	public static ImagePlus boundaryZone(ImagePlus seg, int zoneIn, int zoneOut,int connexity,int outValue) {
		int X=seg.getWidth();
		int Y=seg.getHeight();
		int Z=seg.getStackSize();
		byte[][] valSeg=new byte[Z][];		
		byte[][] valOut=new byte[Z][];		
		ImagePlus out=VitimageUtils.imageCopy(seg);
		for(int z=0;z<Z;z++) {
			valSeg[z]=(byte []) seg.getStack().getProcessor(z+1).getPixels();
			valOut[z]=(byte []) out.getStack().getProcessor(z+1).getPixels();
		}

		boolean touche;
		int[]tab=new int[connexity];
		for(int z=1;z<Z-1;z++) {
			for(int x=1;x<X-1;x++) {
				for(int y=1;y<Y-1;y++) {
					touche=false;
					int catIn=(int)(valSeg[z][y*X+x] & 0xff);
					if(catIn!=zoneIn) {
						valOut[z][y*X+x]=(   ((byte) (0 & 0xff)) );
						continue;
					}
					if(connexity==4) {
						tab[0]=(int)(valSeg[( z )][( y )*X+( x+1 )] & 0xff);
						tab[1]=(int)(valSeg[( z )][( y+1 )*X+( x )] & 0xff);
						tab[2]=(int)(valSeg[( z )][( y )*X+( x-1 )] & 0xff);
						tab[3]=(int)(valSeg[( z )][( y-1 )*X+( x )] & 0xff);
					}
					if(connexity==8) {
						tab[0]=(int)(valSeg[( z )][( y )*X+( x+1 )] & 0xff);
						tab[1]=(int)(valSeg[( z )][( y+1 )*X+( x+1 )] & 0xff);
						tab[2]=(int)(valSeg[( z )][( y+1 )*X+( x )] & 0xff);
						tab[3]=(int)(valSeg[( z )][( y+1 )*X+( x-1 )] & 0xff);

						tab[4]=(int)(valSeg[( z )][( y )*X+( x-1 )] & 0xff);
						tab[5]=(int)(valSeg[( z )][( y-1 )*X+( x-1 )] & 0xff);
						tab[6]=(int)(valSeg[( z )][( y-1 )*X+( x )] & 0xff);
						tab[7]=(int)(valSeg[( z )][( y-1 )*X+( x+1 )] & 0xff);
					}
					if(connexity==6) {
						tab[0]=(int)(valSeg[( z )][( y )*X+( x+1 )] & 0xff);
						tab[1]=(int)(valSeg[( z )][( y )*X+( x-1 )] & 0xff);
						tab[2]=(int)(valSeg[( z )][( y+1 )*X+( x )] & 0xff);
						tab[3]=(int)(valSeg[( z )][( y-1 )*X+( x )] & 0xff);
						tab[4]=(int)(valSeg[( z+1 )][( y )*X+( x )] & 0xff);
						tab[5]=(int)(valSeg[( z-1 )][( y )*X+( x )] & 0xff);
					}
					if(connexity==26) {
						tab[0]=(int)(valSeg[( z )][( y )*X+( x+1 )] & 0xff);
						tab[1]=(int)(valSeg[( z )][( y+1 )*X+( x+1 )] & 0xff);
						tab[2]=(int)(valSeg[( z )][( y+1 )*X+( x )] & 0xff);
						tab[3]=(int)(valSeg[( z )][( y+1 )*X+( x-1 )] & 0xff);

						tab[4]=(int)(valSeg[( z )][( y )*X+( x-1 )] & 0xff);
						tab[5]=(int)(valSeg[( z )][( y-1 )*X+( x-1 )] & 0xff);
						tab[6]=(int)(valSeg[( z )][( y-1 )*X+( x )] & 0xff);
						tab[7]=(int)(valSeg[( z )][( y-1 )*X+( x+1 )] & 0xff);

						tab[8+0]=(int)(valSeg[( z-1 )][( y )*X+( x+1 )] & 0xff);
						tab[8+1]=(int)(valSeg[( z-1 )][( y+1 )*X+( x+1 )] & 0xff);
						tab[8+2]=(int)(valSeg[( z-1 )][( y+1 )*X+( x )] & 0xff);
						tab[8+3]=(int)(valSeg[( z-1 )][( y+1 )*X+( x-1 )] & 0xff);

						tab[8+4]=(int)(valSeg[( z-1 )][( y )*X+( x-1 )] & 0xff);
						tab[8+5]=(int)(valSeg[( z-1 )][( y-1 )*X+( x-1 )] & 0xff);
						tab[8+6]=(int)(valSeg[( z-1 )][( y-1 )*X+( x )] & 0xff);
						tab[8+7]=(int)(valSeg[( z-1 )][( y-1 )*X+( x+1 )] & 0xff);

						tab[16+0]=(int)(valSeg[( z+1 )][( y )*X+( x+1 )] & 0xff);
						tab[16+1]=(int)(valSeg[( z+1 )][( y+1 )*X+( x+1 )] & 0xff);
						tab[16+2]=(int)(valSeg[( z+1 )][( y+1 )*X+( x )] & 0xff);
						tab[16+3]=(int)(valSeg[( z+1 )][( y+1 )*X+( x-1 )] & 0xff);

						tab[16+4]=(int)(valSeg[( z+1 )][( y )*X+( x-1 )] & 0xff);
						tab[16+5]=(int)(valSeg[( z+1 )][( y-1 )*X+( x-1 )] & 0xff);
						tab[16+6]=(int)(valSeg[( z+1 )][( y-1 )*X+( x )] & 0xff);
						tab[16+7]=(int)(valSeg[( z+1 )][( y-1 )*X+( x+1 )] & 0xff);

						tab[24]=(int)(valSeg[( z+1 )][( y )*X+( x )] & 0xff);
						tab[25]=(int)(valSeg[( z-1 )][( y )*X+( x )] & 0xff);
					}
					if(connexity==18) {
						tab[0]=(int)(valSeg[( z )][( y )*X+( x+1 )] & 0xff);
						tab[1]=(int)(valSeg[( z )][( y+1 )*X+( x+1 )] & 0xff);
						tab[2]=(int)(valSeg[( z )][( y+1 )*X+( x )] & 0xff);
						tab[3]=(int)(valSeg[( z )][( y+1 )*X+( x-1 )] & 0xff);

						tab[4]=(int)(valSeg[( z )][( y )*X+( x-1 )] & 0xff);
						tab[5]=(int)(valSeg[( z )][( y-1 )*X+( x-1 )] & 0xff);
						tab[6]=(int)(valSeg[( z )][( y-1 )*X+( x )] & 0xff);
						tab[7]=(int)(valSeg[( z )][( y-1 )*X+( x+1 )] & 0xff);

						tab[8+0]=(int)(valSeg[( z-1 )][( y )*X+( x+1 )] & 0xff);
						tab[8+1]=(int)(valSeg[( z-1 )][( y+1 )*X+( x )] & 0xff);
						tab[8+2]=(int)(valSeg[( z-1 )][( y )*X+( x-1 )] & 0xff);
						tab[8+3]=(int)(valSeg[( z-1 )][( y-1 )*X+( x )] & 0xff);

						tab[12+0]=(int)(valSeg[( z-1 )][( y )*X+( x+1 )] & 0xff);
						tab[12+1]=(int)(valSeg[( z-1 )][( y+1 )*X+( x )] & 0xff);
						tab[12+2]=(int)(valSeg[( z-1 )][( y )*X+( x-1 )] & 0xff);
						tab[12+3]=(int)(valSeg[( z-1 )][( y-1 )*X+( x )] & 0xff);

						tab[16]=(int)(valSeg[( z+1 )][( y )*X+( x )] & 0xff);
						tab[17]=(int)(valSeg[( z-1 )][( y )*X+( x )] & 0xff);
					}
					boolean debug=false;
					for(int i=0;i<tab.length;i++) {
						int val=tab[i];
						if(val!=zoneOut) {
							if(debug) {
								System.out.println("FAUX A : "+x+","+y+","+z);							
								System.out.println("tab[i]="+val);
								System.out.println("zoneIn="+zoneIn);
								System.out.println("zoneOut="+zoneOut);
								System.out.println(TransformUtils.stringVectorN(tab, "vals neigh"));
							}
						}
						else {
							if(debug) {
								System.out.println("VRAI A : "+x+","+y+","+z);
								System.out.println("tab[i]="+val);
								System.out.println("zoneIn="+zoneIn);
								System.out.println("zoneOut="+zoneOut);
								System.out.println(TransformUtils.stringVectorN(tab, "vals neigh"));
							}
							touche=true;
						}
					}
					valOut[z][y*X+x]=(   ((byte) ((touche ? outValue : 0) & 0xff)) );
				}			 
			}
		}
		return out;
	}
	

	public static ImagePlus thresholdByteImage(ImagePlus img,double thresholdMin, double thresholdMax) {
		ImagePlus ret=new Duplicator().run(img);
		VitimageUtils.adjustImageCalibration(ret,img);
		byte[][] in=new byte[img.getStackSize()][];
		byte[][] out=new byte[ret.getStackSize()][];
		int X=img.getWidth();
		int Y=img.getHeight();
		int Z=img.getStackSize();
		for(int z=0;z<Z;z++) {
			in[z]=(byte []) img.getStack().getProcessor(z+1).getPixels();
			out[z]=(byte []) ret.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<X;x++) {
				for(int y=0;y<Y;y++) {
					int val=(int)(in[z][y*X+x] & 0xff);
					out[z][y*X+x]=(  val >= thresholdMin ? (        val < thresholdMax  ?   ((byte) (255 & 0xff)) : ((byte) (0 & 0xff)) ) : ((byte) (0 & 0xff)) );
				}			 
			}
		}
		return ret;

	}
	
	
	
	/**
	 * Connected components utilities
	 * @param img
	 * @param computeZaxisOnly
	 * @param cornersCoordinates
	 * @param ignoreUnattemptedDimensions
	 * @return
	 */
	public static ImagePlus connexe(ImagePlus img,double threshLow,double threshHigh,double volumeLow,double volumeHigh,int connexity,int selectByVolume,boolean noVerbose) {
		boolean debug=!noVerbose;
		if(debug)System.out.println("Depart connexe");
		int yMax=img.getHeight();
		int xMax=img.getWidth();
		int zMax=img.getStack().getSize();
		double vX=img.getCalibration().pixelWidth;
		double vY=img.getCalibration().pixelHeight;
		double vZ=img.getCalibration().pixelDepth;
		double voxVolume=vX*vY*vZ;
		int[][]connexions;
		int[]volume;
		if(debug)System.out.println("Allocations 1, en MegaInt : "+(0.000001*xMax*yMax*zMax));
		int[][][]tabIn=new int[xMax][yMax][zMax];
		if(0.000001*xMax*yMax*zMax>20) {
			if(debug)System.out.println("Allocations 2, en MegaInt : "+(0.000001*xMax*yMax*zMax/20));
			connexions=new int[xMax*yMax*zMax/20][2];
			if(debug)System.out.println("Allocations 3, en MegaInt : "+(0.000001*xMax*yMax*zMax/20));
			volume=new int[xMax*yMax*zMax/20];
		}
		else {
			if(debug)System.out.println("Allocations 2, en MegaInt : "+(0.000001*xMax*yMax*zMax/5));
			connexions=new int[xMax*yMax*zMax/5][2];
			if(debug)System.out.println("Allocations 3, en MegaInt : "+(0.000001*xMax*yMax*zMax/5));
			volume=new int[xMax*yMax*zMax/5];
		}
		int[][]neighbours=new int[][]{{1,0,0,0},{1,1,0,0},{0,1,0,0},{0,0,1,0},{1,0,1,0},{1,1,1,0},{0,1,1,0} };
		int curComp=0;
		int indexConnexions=0;
		if(debug)System.out.println("Choix d'un type");
		switch(img.getType()) {
		case ImagePlus.GRAY8:
			byte[] imgInB;
			for(int z=0;z<zMax;z++) {
				imgInB=(byte[])(img.getStack().getProcessor(z+1).getPixels());
				for(int x=0;x<xMax;x++)for(int y=0;y<yMax;y++)if(  ((float)(imgInB[x+xMax*y] & 0xff) < threshHigh )  && ((float)((imgInB[x+xMax*y]) & 0xff) >= threshLow) )tabIn[x][y][z]=-1;
			}
			break;
		case ImagePlus.GRAY16:
			short[] imgInS;
			for(int z=0;z<zMax;z++) {
				imgInS=(short[])(img.getStack().getProcessor(z+1).getPixels());
				for(int x=0;x<xMax;x++)for(int y=0;y<yMax;y++)if(  ((float)(imgInS[x+xMax*y] & 0xffff) < threshHigh )  && ((float)((imgInS[x+xMax*y]) & 0xffff) >=threshLow) )tabIn[x][y][z]=-1;					
			}
			break;
		case ImagePlus.GRAY32:
			float[] imgInF;
			for(int z=0;z<zMax;z++) {
				imgInF=(float[])(img.getStack().getProcessor(z+1).getPixels());
				for(int x=0;x<xMax;x++)for(int y=0;y<yMax;y++)if(  ((imgInF[x+xMax*y]) < threshHigh )  && (((imgInF[x+xMax*y])) >= threshLow) )tabIn[x][y][z]=-1;					
			}
			break;
		}

		if(debug)System.out.println("Boucle principale");
		//Boucle principale
		for(int x=0;x<xMax;x++) {
			for(int y=0;y<yMax;y++) {
				for(int z=0;z<zMax;z++) {
					if(tabIn[x][y][z]==0)continue;//Point du fond
					if(tabIn[x][y][z]==-1) {
						tabIn[x][y][z]=(++curComp);//New object
						if(curComp==volume.length) {
							//agrandir le tableau de volumes de 20 %
							int[]volumeBigger=new int[(14*volume.length)/10];
							if(debug)System.out.println("Volumes array touch to limit. Raising size to "+(14*volume.length)/10);
							for(int i=0;i<curComp;i++) {
								volumeBigger[i]=volume[i];
								volumeBigger[i]=volume[i];
							}
							volume=volumeBigger;
						}

						volume[curComp]++;
					}
					if(tabIn[x][y][z]>0) {//Here we need to explore the neighbours
						for(int nei=0;nei<7;nei++)neighbours[nei][3]=1;//At the beginning, every neighbour is possible. 
						//    Z axis
						//     /|\ 6---------5         
						//      | /|        /|
						//      |/ |       / |  
						//      3---------4  |
						//      |  |      |  |
						//      |  2------|--1
						//      | /       | /
						//      |/        |/
						//      X---------0-----> X axis
 						//
						//Then we need to reduce access according to images dims and chosen connexity
						if(x==xMax-1)neighbours[0][3]=neighbours[1][3]=neighbours[4][3]=neighbours[5][3]=0;
						if(y==yMax-1)neighbours[1][3]=neighbours[2][3]=neighbours[5][3]=neighbours[6][3]=0;
						if(z==zMax-1)neighbours[3][3]=neighbours[4][3]=neighbours[5][3]=neighbours[6][3]=0;
						if(connexity==4)neighbours[1][3]=neighbours[3][3]=neighbours[4][3]=neighbours[5][3]=neighbours[6][3]=0;
						if(connexity==6)neighbours[1][3]=neighbours[4][3]=neighbours[5][3]=neighbours[6][3]=0;
						if(connexity==8)neighbours[3][3]=neighbours[4][3]=neighbours[5][3]=neighbours[6][3]=0;
						if(connexity==18)neighbours[5][3]=0;

						//Given these neighbours, we can visit them
						for(int nei=0;nei<7;nei++) {
							if(neighbours[nei][3]==1) {
								//System.out.println("Go, avec nei="+nei+" x="+x+" y="+y+" z"+z+" NEIS={"+neighbours[nei][0]+","+neighbours[nei][1]+","+neighbours[nei][2]"}");
								if(tabIn[x+neighbours[nei][0]][y+neighbours[nei][1]][z+neighbours[nei][2]]==0)continue;
								if(tabIn[x+neighbours[nei][0]][y+neighbours[nei][1]][z+neighbours[nei][2]]==-1) {
									tabIn[x+neighbours[nei][0]][y+neighbours[nei][1]][z+neighbours[nei][2]]=tabIn[x][y][z];
									volume[tabIn[x][y][z]]++;
								}
								else {
									if(indexConnexions==connexions.length) {
										//agrandir le tableau de connexions de 20 %
										int[][]connexionsBigger=new int[(14*connexions.length)/10][2];
										if(debug)System.out.println("Connexions array touch to limit. Raising size to "+(14*connexions.length)/10);
										for(int i=0;i<indexConnexions;i++) {
											connexionsBigger[i][0]=connexions[i][0];
											connexionsBigger[i][1]=connexions[i][1];
										}
										connexions=connexionsBigger;
									}
									connexions[indexConnexions][0]=tabIn[x+neighbours[nei][0]][y+neighbours[nei][1]][z+neighbours[nei][2]];
									connexions[indexConnexions++][1]=tabIn[x][y][z];
								}
							}
						}
					}
				}	
			}			
		}

		//System.out.println("Resolution des conflits entre groupes connexes");
		//Resolution des groupes d'objets connectes entre eux (formes en U, et cas plus compliqués)
		int[]lut = resolveConnexitiesGroupsAndExclude(connexions,indexConnexions,curComp+1,volume,volumeLow/voxVolume,volumeHigh/voxVolume,selectByVolume,noVerbose);


		//Build computed image of objects
		ImagePlus imgOut=ij.gui.NewImage.createImage(img.getShortTitle()+"_"+connexity+"CON",xMax,yMax,zMax,16,ij.gui.NewImage.FILL_BLACK);
		short[] imgOutTab;
		for(int z=0;z<zMax;z++) {
			imgOutTab=(short[])(imgOut.getStack().getProcessor(z+1).getPixels());
			for(int x=0;x<xMax;x++)for(int y=0;y<yMax;y++) imgOutTab[x+xMax*y] = (short) ( lut[tabIn[x][y][z]]  );
		}
		imgOut.getProcessor().setMinAndMax(0,lut[curComp+1]);
		VitimageUtils.adjustImageCalibration(imgOut, img);
		return imgOut;	
	}
	
	public static int[] resolveConnexitiesGroupsAndExclude(int  [][] connexions,int nbCouples,int n,int []volume,double volumeLowP,double volumeHighP,int selectByVolume,boolean noVerbose) {
		int[]prec=new int[n];
		int[]lut=new int[n+1];
		int[]next=new int[n];
		int[]label=new int[n];
		for(int i=0;i<n;i++) {label[i]=i;prec[i]=0;next[i]=0;}

		int indA,indB,valMin,valMax,indMin,indMax;
		for(int couple=0;couple<nbCouples;couple++) {
			indA=connexions[couple][0];
			indB=connexions[couple][1];
			if(label[indA]==label[indB])continue;
			if(label[indA]<label[indB]) {
				valMin=label[indA];
				indMin=indA;
				valMax=label[indB];
				indMax=indB;
			}
			else {
				valMin=label[indB];
				indMin=indB;
				valMax=label[indA];
				indMax=indA;
			}
			while(next[indMin]>0)indMin=next[indMin];
			while(prec[indMax]>0)indMax=prec[indMax];
			prec[indMax]=indMin;
			next[indMin]=indMax;
			while(next[indMin]>0) {
				indMin=next[indMin];
				label[indMin]=valMin;
			}
		}
		//Compute number of objects and volume
		for (int i=1;i<n ;i++){
			if(label[i]!=i) {
				volume[label[i]]+=volume[i];
				volume[i]=0;
			}
		}
		//copy and sort volumes
		Object [][]tabSort=new Object[n][2];
		int selectedIndex=0;
		for (int i=0;i<n ;i++) {
			tabSort[i][0]=new Double(volume[i]);
			tabSort[i][1]=new Integer(i);
		}


		Arrays.sort(tabSort,new VolumeComparator());
		if(selectByVolume>n)selectByVolume=n;
		if(selectByVolume<1)selectByVolume=0;
		if(selectByVolume!=0)selectedIndex=((Integer)(tabSort[n-selectByVolume][1])).intValue();

		//Exclude too big or too small objects,
		int displayedValue=1;
		for (int i=1;i<n ;i++){
			if(selectByVolume!=0) {
				if(i==selectedIndex)lut[i]=255;
				else lut[i]=0;
			}
			else if( (volume[i]>0) && (volume[i]>=volumeLowP) && (volume[i]<=volumeHighP) ) {
				lut[i]=displayedValue++;
			}
		}
		if(displayedValue>65000) {System.out.println("Warning : connexe , "+(displayedValue-1)+" connected components");}
		else if(! noVerbose)System.out.println("Number of connected components detected : "+(selectByVolume>0 ? 1 : (displayedValue-1)));

		//Group labels
		for (int i=0;i<n ;i++){
			lut[i]=lut[label[i]];
		}
		//Tricky little parameters to provide a good display after operation;
		if(selectByVolume !=0)lut[n]=255;
		else lut[n]=displayedValue;
		return lut;
	}

	public static int[][][]getContactPointsAs3DArrayInLittleVoxels(double targetVoxelSize,int numSpec,ImagePlus imgArea,boolean circle){
		int[][][]ret=null;
		if(!circle) {
			int[][]tab=getContactPoints(0,0)[numSpec];
			ret=new int[tab.length][][];
			for(int d1=0;d1<tab.length;d1++) {
				ret[d1]=new int[1][3];
				ret[d1][0][0]=(int)Math.round(tab[d1][0]*0.7224/targetVoxelSize);
				ret[d1][0][1]=(int)Math.round(tab[d1][1]*0.7224/targetVoxelSize);
				ret[d1][0][2]=(int)Math.round(tab[d1][2]*1/targetVoxelSize);
			}
		}
		return ret;
	}
	
	public static int[][][]getContactPoints(int i, int j){
		return new int[][][] {
		{  { 146,55  , 60},{118, 188  , 60},{9,126   ,60} },/*AS1 la premiere elle est pas passee  */			
		{  {42,146,62},{52,202,62}},/*AS2*/	
		{  {110,163,62},{105,107,71}},/*AS3*/	
		{  {6,154,27},{60,126,27},{158,39,27} },/*RES1*/	
		{  {224,103,22},{165,199,51},{268,35,78} },/*RES2*/	
		{  {84,185,97},{7,147,110},{116,125,46} },/*RES3 gris, marron, vert*/	
		{  {100-4,135-12,23},{133,84,25}},/*{ //  {100+i,135+j,23}},// {100-3,130-7,25},{133,84,25}}S1 marron, vert le premier n a pas marche     ok*/	
		{  {138,62,42},{224,208,42} },/*S2 vert, marron*/	
		{  {7,158,102},{19,80,90},{239,111,11} },/* {7,158,102},{10,102,77},{236,113,11}S3 transpa, le deuxieme ne prend pas*/	
		{  {61,199,44},{62,129,44} },/*APO1*/	
		{  {102,54,22} },/*APO2*/	
		{  {15,136,56},{224,193,56} }/*APO3*/	
	};
/*		int[][][]contactPointsNecrose=new int[][][] {
		{  { 197,109  , 88} }			//,{18, 141  , 41}
		{  {0,0,0},{0,0,0},{0,0,0} }
	};*/
	}	

	
	public static ImagePlus[]getImagePlusStackAsImagePlusTab(ImagePlus img){
		ImagePlus[]ret=new ImagePlus[img.getStackSize()];
		for(int i=0;i<img.getStackSize();i++) {
			img.setSlice(i+1);
			ret[i] =  img.crop();
			VitimageUtils.adjustImageCalibration(ret[i],img);
		}
		return ret;
	}

	/**
	 * Main automated detectors : axis detection and inoculation point detection. Usable for both MRI T1, T2, and X ray images
	 * @param img1
	 * @param acqType
	 * @return
	 */
	public static Point3d[] detectAxis(ImagePlus img1,AcquisitionType acqType){
		ImagePlus img=new Duplicator().run(img1);
		boolean debug=false;
		int xMax=img.getWidth();
		int yMax=img.getHeight();
		int zMax=img.getStackSize();
		double vX=img.getCalibration().pixelWidth;
		double vY=img.getCalibration().pixelHeight;
		double vZ=img.getCalibration().pixelDepth;
		double xMoyUp=0,yMoyUp=0,zMoyUp=0;
		double xMoyDown=1,yMoyDown=1,zMoyDown=1;
		int hitsUp=0,hitsDown=0;

		//Step 1 : apply gaussian filtering and convert to 8 bits
		if(acqType != AcquisitionType.RX)img=VitimageUtils.gaussianFiltering(img, 18*0.035 , 18*0.035 , 3*0.5);
		img.getProcessor().setMinAndMax(
				acqType==AcquisitionType.MRI_T1_SEQ ? 200 : 10000, 
				acqType==AcquisitionType.MRI_T1_SEQ ? 3000 : 50000);
		StackConverter sc=new StackConverter(img);
		sc.convertToGray8();
		if(debug)imageChecking(img,"fin step1 ");

		//Step 2 : apply automatic threshold
		ByteProcessor[] maskTab=new ByteProcessor[zMax];

		if(acqType == AcquisitionType.RX)img=VitimageUtils.eraseBorder(img);
		if(debug)imageChecking(img,"after Erase ");
		if(acqType != AcquisitionType.MRI_T2_SEQ) {
			
			System.out.println("Mask lookup for center of object, case of hign SNR (T1 or RX)");
			for(int z=0;z<zMax;z++){
				maskTab[z]=(ByteProcessor) img.getStack().getProcessor(z+1);
				maskTab[z].setAutoThreshold("Otsu dark");
				maskTab[z]=maskTab[z].createMask();
			}
		}
		else {
			System.out.println("Mask lookup for center of object, case of low SNR (T2)");
			for(int z=0;z<zMax;z++){
				maskTab[z]=(ByteProcessor) img.getStack().getProcessor(z+1);
				maskTab[z].setThreshold(20,255,1);
				maskTab[z]=maskTab[z].createMask();
			}
			
		}
			
			
		//Step 2.1 : Extract two substacks for the upper part and the lower part of the object
		ImageStack stackUp = new ImageStack(xMax, yMax);	
		ImageStack stackDown = new ImageStack(xMax, yMax);
		int zQuarter=zMax/4;
		int zVentile=zMax/40;
		zVentile=(zVentile < 10 ? 10 : zVentile);
		if(zMax<zVentile*2+2)zVentile=zMax/2-1;
		for(int i=0;i<zVentile;i++) {
			stackUp.addSlice("",maskTab[zMax/2+zQuarter-zVentile+i] );//de zmax/2 +zQuarter-zVentile à zMax/2 + zQuarter-zVentile5 --> ajouter zMax/2 à la fin			
			stackDown.addSlice("",maskTab[zMax/2-zQuarter+i+1] );//de zmax/2-5 à zMax/2   --> ajouter zMax/2-5 à la fin
		}
		ImagePlus imgUp=new ImagePlus("upMASK",stackUp);
		VitimageUtils.adjustImageCalibration(imgUp, img);
		if(debug)imageChecking(imgUp);				
		ImagePlus imgUpCon=connexe(imgUp,0,29,0,10E10,6,2,false);
		if(debug)imageChecking(imgUpCon,"imgUpCon");
		
		ImagePlus imgDown=new ImagePlus("downMASK",stackDown);
		VitimageUtils.adjustImageCalibration(imgDown, img);
		if(debug)imageChecking(imgDown);
		ImagePlus imgDownCon=connexe(imgDown,0,29,0,10E10,6,2,false);
		if(debug)imageChecking(imgDownCon,"imgDownCon");
		IJ.saveAsTiff(imgUpCon,"/home/fernandr/Bureau/pouet.tif");
		
		if(VitimageUtils.isNullImage(imgUpCon)) {
			System.out.println("Handling case of void moelle");
			if(debug)imageChecking(imgUp,"Up init");
			imgUpCon=VitimageUtils.gaussianFiltering(imgUp, 30*vX, 30*vY, 3*vZ);
			if(debug)imageChecking(imgUpCon,"Apres filtrage");
			//imgUpCon.show();
			//VitimageUtils.waitFor(10000);
			imgUpCon=VitimageUtils.getBinaryMask(imgUpCon, 253);
			if(debug)imageChecking(imgUpCon,"Apres seuillage");
			imgUpCon=connexe(imgUpCon,30,256,0,10E10,6,1,false);
			if(debug)imageChecking(imgUpCon,"Apres connexe");
		}

		if(VitimageUtils.isNullImage(imgDownCon)) {
			System.out.println("Handling case of void moelle");
			if(debug)imageChecking(imgDown,"Down init");
			imgDownCon=VitimageUtils.gaussianFiltering(imgDown, 30*vX, 30*vY, 3*vZ);
			if(debug)imageChecking(imgDownCon,"Apres filtrage");
			imgDownCon=VitimageUtils.getBinaryMask(imgDownCon, 254);
			if(debug)imageChecking(imgDownCon,"Apres seuillage");
			imgDownCon=connexe(imgDownCon,30,256,0,10E10,6,1,false);
			if(debug)imageChecking(imgDownCon,"Apres connexe");
		}
		
		System.out.println("There");
		
		//Step 3 : compute the two centers of mass
		short[][]valsDownCon=new short[zQuarter][];
		short[][]valsUpCon=new short[zQuarter][];
		for(int z=0;z<zVentile;z++){
			valsDownCon[z]=(short[])(imgDownCon).getStack().getProcessor(z+1).getPixels();
			valsUpCon[z]=(short[])(imgUpCon).getStack().getProcessor(z+1).getPixels();
		}

		for(int x=0;x<xMax;x++){
			for(int y=0;y<yMax;y++){
				for(int z=0;z<zVentile;z++){								
					if(valsDownCon[z][xMax*y+x]==((short)255)){//We are in the first part of the object
						hitsDown++;
						xMoyDown+=x;yMoyDown+=y;zMoyDown+=z;
					}
					if(valsUpCon[z][xMax*y+x]==((short	)255)){//We are in the first part of the object
						hitsUp++;
						xMoyUp+=x;yMoyUp+=y;zMoyUp+=z;
					}
				}
			}
		}
		System.out.println("Here");

		if(hitsUp==0)hitsUp=1;
		if(hitsDown==0)hitsDown=1;
		xMoyUp=xMoyUp/hitsUp;//Center of mass computation. 
		yMoyUp=yMoyUp/hitsUp;//Double type stands a 15 digits precisions; which is enough here, until #voxels < 5.10^12 
		zMoyUp=zMoyUp/hitsUp+zMax/2+zQuarter-zVentile;//due to the extraction of a substack zmax/2-zQuarter+1 - zmax/2     zMax/2+zQuarter-zVentile

		xMoyDown=xMoyDown/hitsDown;//Center of mass computation. 
		yMoyDown=yMoyDown/hitsDown;//Double type stands a 15 digits precisions; which is enough here, until #voxels < 5.10^12 
		zMoyDown=zMoyDown/hitsDown+zMax/2-zQuarter+1;//due to the extraction of a substack zmax/2 - zmax/2+zQuarter       zMax/2-zQuarter+1
		debug =true;
		if(debug) {
			System.out.println("HitsUp="+hitsUp+" ..Center of mass up = "+xMoyUp+"  ,  "+yMoyUp+"  ,  "+zMoyUp);
			System.out.println("HitsDown="+hitsDown+" ..Center of mass down = "+xMoyDown+"  ,  "+yMoyDown+"  ,  "+zMoyDown);
		}
		System.out.println("Here");

		xMoyUp=xMoyUp*vX;		
		yMoyUp=yMoyUp*vY;		
		zMoyUp=zMoyUp*vZ;	
		xMoyDown=xMoyDown*vX;		
		yMoyDown=yMoyDown*vY;		
		zMoyDown=zMoyDown*vZ;		
		if(debug) {
			System.out.println("Center of mass up (coord reel)= "+xMoyUp+"  ,  "+yMoyUp+"  ,  "+zMoyUp);
			System.out.println("Center of mass down (coord reel)= "+xMoyDown+"  ,  "+yMoyDown+"  ,  "+zMoyDown);
		}
		System.out.println("THere");

		//Step 4 : compute the axis vector, that will stands for Z vector after alignement
		double[]vectZ=TransformUtils.normalize(new double[] {xMoyUp - xMoyDown , yMoyUp - yMoyDown , zMoyUp - zMoyDown});
		double[][]axisVerificationMatrix=VitiDialogs.inspectAxis( img1 ,vectZ,new Point3d(xMoyUp*0.5+xMoyDown*0.5 , yMoyUp*0.5+yMoyDown*0.5 , zMoyUp*0.5+zMoyDown*0.5 ),0);
		double []vectZbis=axisVerificationMatrix[0];
		double epsilon=0.0000001;
		if(TransformUtils.norm(TransformUtils.vectorialSubstraction(vectZbis,vectZ))>epsilon) {//Une erreur a été corrigée par l'utilisateur
			System.out.println("En effet il y a eu modification");
			xMoyUp=axisVerificationMatrix[1][0];xMoyDown=axisVerificationMatrix[2][0];
			yMoyUp=axisVerificationMatrix[1][1];yMoyDown=axisVerificationMatrix[2][1];
			zMoyUp=axisVerificationMatrix[1][2];zMoyDown=axisVerificationMatrix[2][2];
			vectZ=vectZbis;
		}

		
		System.out.println("Vecteur axial ="+TransformUtils.stringVector(vectZ,""));
		double []vectXtmp=new double[] {1,0,0};
		double []vectX=TransformUtils.normalize(TransformUtils.vectorialSubstraction(vectXtmp, TransformUtils.proj_u_of_v(vectZ, vectXtmp)));
		System.out.println("Vecteur orthogonal ="+TransformUtils.stringVector(vectX,""));

		
		
		

		//Step 5 : compute the three points
		Point3d origine=new Point3d(xMoyUp*0.5+xMoyDown*0.5 , yMoyUp*0.5+yMoyDown*0.5 , zMoyUp*0.5+zMoyDown*0.5 );
		Point3d ptUp= new Point3d(origine.x + vectZ[0]   ,  origine.y + vectZ[1] , origine.z + vectZ[2]);
		Point3d ptRight= new Point3d(origine.x + vectX[0]   ,  origine.y + vectX[1] , origine.z + vectX[2]);
		return new Point3d[] {origine,ptUp,ptRight};
	}

	public static String imageResume(ImagePlus img) {
		if(img==null)return "image est nulle";
		int[]dims=VitimageUtils.getDimensions(img);
		double[]voxs=VitimageUtils.getVoxelSizes(img);
		String s="Image "+img.getTitle()+" coded on "+img.getBitDepth()+" per pixel. Dims="+dims[0]+" X "+dims[1]+" X "+dims[2]+"  VoxS="+voxs[0]+" x "+voxs[1]+" x "+voxs[2]+"  NChannels="+img.getNChannels()+"  NFrames="+img.getNFrames();
		return s;
	}

	public static void printImageResume(ImagePlus img) {
		System.out.println(imageResume(img));
	}

	public static void printImageResume(ImagePlus img,String str) {
		System.out.println(str+" : "+imageResume(img));
	}

	
	public static int max(int[]tab){
		if(tab.length==0) {
			IJ.log("In VitimageUtils.max, tab is null. Return 0");
			return 0;
		}
		int max=tab[0];
		for(int i=1;i<tab.length;i++) {
			if(tab[i]>max)max=tab[i];
		}
		return max;
	}

	public static double max(double[]tab){
		if(tab.length==0) {
			IJ.log("In VitimageUtils.max, tab is null. Return 0");
			return 0;
		}
		double max=tab[0];
		for(int i=1;i<tab.length;i++) {
			if(tab[i]>max)max=tab[i];
		}
		return max;
	}
	public static int min(int[]tab){
		if(tab.length==0) {
			IJ.log("In VitimageUtils.min, tab is null. Return 0");
			return 0;
		}
		int min=tab[0];
		for(int i=1;i<tab.length;i++) {
			if(tab[i]<min)min=tab[i];
		}
		return min;
	}
	
	public static double min(double[]tab){
		if(tab.length==0) {
			IJ.log("In VitimageUtils.min, tab is null. Return 0");
			return 0;
		}
		double min=tab[0];
		for(int i=1;i<tab.length;i++) {
			if(tab[i]<min)min=tab[i];
		}
		return min;
	}
	
	
	
	public static Point3d[] detectAxisIrmT1(ImagePlus img,int delayForReacting){
		boolean debug=false;
		int xMax=img.getWidth();
		int yMax=img.getHeight();
		int zMax=img.getStackSize();
		double vX=img.getCalibration().pixelWidth;
		double vY=img.getCalibration().pixelHeight;
		double vZ=img.getCalibration().pixelDepth;
		double xMoyUp=0,yMoyUp=0,zMoyUp=0;
		double xMoyDown=1,yMoyDown=1,zMoyDown=1;
		int hitsUp=0,hitsDown=0;

		//Step 1 : apply gaussian filtering and convert to 8 bits
		img=VitimageUtils.gaussianFiltering(img, 18*0.035 , 18*0.035 , 3*0.5);
		img.getProcessor().setMinAndMax(0,1);
		StackConverter sc=new StackConverter(img);
		sc.convertToGray8();
		if(debug)imageChecking(img,"fin step1 ");

		//Step 2 : apply automatic threshold
		ByteProcessor[] maskTab=new ByteProcessor[zMax];

		System.out.println("Mask lookup for center of object, case of hign SNR (T1 or RX)");
		for(int z=0;z<zMax;z++){
			maskTab[z]=(ByteProcessor) img.getStack().getProcessor(z+1);
			maskTab[z].setAutoThreshold("Otsu dark");
			maskTab[z]=maskTab[z].createMask();
		}
			
			
		//Step 2.1 : Extract two substacks for the upper part and the lower part of the object
		ImageStack stackUp = new ImageStack(xMax, yMax);	
		ImageStack stackDown = new ImageStack(xMax, yMax);
		int zQuarter=zMax/4;
		int zVentile=zMax/40;
		zVentile=(zVentile < 10 ? 10 : zVentile);
		if(zMax<zVentile*2+2)zVentile=zMax/2-1;
		for(int i=0;i<zVentile;i++) {
			stackUp.addSlice("",maskTab[zMax/2+zQuarter-zVentile+i] );//de zmax/2 +zQuarter-zVentile à zMax/2 + zQuarter-zVentile5 --> ajouter zMax/2 à la fin			
			stackDown.addSlice("",maskTab[zMax/2-zQuarter+i+1] );//de zmax/2-5 à zMax/2   --> ajouter zMax/2-5 à la fin
		}
		ImagePlus imgUp=new ImagePlus("upMASK",stackUp);
		VitimageUtils.adjustImageCalibration(imgUp, img);
		if(debug)imageChecking(imgUp);				
		ImagePlus imgUpCon=connexe(imgUp,0,29,0,10E10,6,2,true);
		if(debug)imageChecking(imgUpCon,"imgUpCon");
		
		ImagePlus imgDown=new ImagePlus("downMASK",stackDown);
		VitimageUtils.adjustImageCalibration(imgDown, img);
		if(debug)imageChecking(imgDown);
		ImagePlus imgDownCon=connexe(imgDown,0,29,0,10E10,6,2,true);
		if(debug)imageChecking(imgDownCon,"imgDownCon");
		IJ.saveAsTiff(imgUpCon,"/home/fernandr/Bureau/pouet.tif");
		
		if(VitimageUtils.isNullImage(imgUpCon)) {
			System.out.println("Handling case of void moelle");
			if(debug)imageChecking(imgUp,"Up init");
			imgUpCon=VitimageUtils.gaussianFiltering(imgUp, 30*vX, 30*vY, 3*vZ);
			if(debug)imageChecking(imgUpCon,"Apres filtrage");
			//imgUpCon.show();
			//VitimageUtils.waitFor(10000);
			imgUpCon=VitimageUtils.getBinaryMask(imgUpCon, 253);
			if(debug)imageChecking(imgUpCon,"Apres seuillage");
			imgUpCon=connexe(imgUpCon,30,256,0,10E10,6,1,true);
			if(debug)imageChecking(imgUpCon,"Apres connexe");
		}

		if(VitimageUtils.isNullImage(imgDownCon)) {
			System.out.println("Handling case of void moelle");
			if(debug)imageChecking(imgDown,"Down init");
			imgDownCon=VitimageUtils.gaussianFiltering(imgDown, 30*vX, 30*vY, 3*vZ);
			if(debug)imageChecking(imgDownCon,"Apres filtrage");
			imgDownCon=VitimageUtils.getBinaryMask(imgDownCon, 254);
			if(debug)imageChecking(imgDownCon,"Apres seuillage");
			imgDownCon=connexe(imgDownCon,30,256,0,10E10,6,1,true);
			if(debug)imageChecking(imgDownCon,"Apres connexe");
		}
		
		
		
		//Step 3 : compute the two centers of mass
		short[][]valsDownCon=new short[zQuarter][];
		short[][]valsUpCon=new short[zQuarter][];
		for(int z=0;z<zVentile;z++){
			valsDownCon[z]=(short[])(imgDownCon).getStack().getProcessor(z+1).getPixels();
			valsUpCon[z]=(short[])(imgUpCon).getStack().getProcessor(z+1).getPixels();
		}

		for(int x=0;x<xMax;x++){
			for(int y=0;y<yMax;y++){
				for(int z=0;z<zVentile;z++){								
					if(valsDownCon[z][xMax*y+x]==((short)255)){//We are in the first part of the object
						hitsDown++;
						xMoyDown+=x;yMoyDown+=y;zMoyDown+=z;
					}
					if(valsUpCon[z][xMax*y+x]==((short	)255)){//We are in the first part of the object
						hitsUp++;
						xMoyUp+=x;yMoyUp+=y;zMoyUp+=z;
					}
				}
			}
		}
		System.out.println("hERE");
		if(hitsUp==0)hitsUp=1;
		if(hitsDown==0)hitsDown=1;
		xMoyUp=xMoyUp/hitsUp;//Center of mass computation. 
		yMoyUp=yMoyUp/hitsUp;//Double type stands a 15 digits precisions; which is enough here, until #voxels < 5.10^12 
		zMoyUp=zMoyUp/hitsUp+zMax/2+zQuarter-zVentile;//due to the extraction of a substack zmax/2-zQuarter+1 - zmax/2     zMax/2+zQuarter-zVentile

		xMoyDown=xMoyDown/hitsDown;//Center of mass computation. 
		yMoyDown=yMoyDown/hitsDown;//Double type stands a 15 digits precisions; which is enough here, until #voxels < 5.10^12 
		zMoyDown=zMoyDown/hitsDown+zMax/2-zQuarter+1;//due to the extraction of a substack zmax/2 - zmax/2+zQuarter       zMax/2-zQuarter+1

		if(debug) {
			System.out.println("HitsUp="+hitsUp+" ..Center of mass up = "+xMoyUp+"  ,  "+yMoyUp+"  ,  "+zMoyUp);
			System.out.println("HitsDown="+hitsDown+" ..Center of mass down = "+xMoyDown+"  ,  "+yMoyDown+"  ,  "+zMoyDown);
		}

		xMoyUp=xMoyUp*vX;		
		yMoyUp=yMoyUp*vY;		
		zMoyUp=zMoyUp*vZ;	
		xMoyDown=xMoyDown*vX;		
		yMoyDown=yMoyDown*vY;		
		zMoyDown=zMoyDown*vZ;		
		if(debug) {
			System.out.println("Center of mass up (coord reel)= "+xMoyUp+"  ,  "+yMoyUp+"  ,  "+zMoyUp);
			System.out.println("Center of mass down (coord reel)= "+xMoyDown+"  ,  "+yMoyDown+"  ,  "+zMoyDown);
		}

		//Step 4 : compute the axis vector, that will stands for Z vector after alignement
		double[]vectZ=TransformUtils.normalize(new double[] {xMoyUp - xMoyDown , yMoyUp - yMoyDown , zMoyUp - zMoyDown});
		double[][]axisVerificationMatrix=VitiDialogs.inspectAxis( img ,vectZ,new Point3d(xMoyUp*0.5+xMoyDown*0.5 , yMoyUp*0.5+yMoyDown*0.5 , zMoyUp*0.5+zMoyDown*0.5 ),0);
		double []vectZbis=axisVerificationMatrix[0];
		double epsilon=0.0000001;
		if(TransformUtils.norm(TransformUtils.vectorialSubstraction(vectZbis,vectZ))>epsilon) {//Une erreur a été corrigée par l'utilisateur
			System.out.println("En effet il y a eu modification");
			xMoyUp=axisVerificationMatrix[1][0];xMoyDown=axisVerificationMatrix[2][0];
			yMoyUp=axisVerificationMatrix[1][1];yMoyDown=axisVerificationMatrix[2][1];
			zMoyUp=axisVerificationMatrix[1][2];zMoyDown=axisVerificationMatrix[2][2];
			vectZ=vectZbis;
		}

		
		System.out.println("Vecteur axial ="+TransformUtils.stringVector(vectZ,""));
		double []vectXtmp=new double[] {1,0,0};
		double []vectX=TransformUtils.normalize(TransformUtils.vectorialSubstraction(vectXtmp, TransformUtils.proj_u_of_v(vectZ, vectXtmp)));
		System.out.println("Vecteur orthogonal ="+TransformUtils.stringVector(vectX,""));

		
		
		

		//Step 5 : compute the three points
		Point3d origine=new Point3d(xMoyUp*0.5+xMoyDown*0.5 , yMoyUp*0.5+yMoyDown*0.5 , zMoyUp*0.5+zMoyDown*0.5 );
		Point3d ptUp= new Point3d(origine.x + vectZ[0]   ,  origine.y + vectZ[1] , origine.z + vectZ[2]);
		Point3d ptRight= new Point3d(origine.x + vectX[0]   ,  origine.y + vectX[1] , origine.z + vectX[2]);
		return new Point3d[] {origine,ptUp,ptRight};
	}

	
	public static ImagePlus cropImageShort(ImagePlus img,int x0,int y0,int z0,int dimX,int dimY,int dimZ) {
		if(img.getType()!=ImagePlus.GRAY16)return null;
		ImagePlus out=ij.gui.NewImage.createImage("Mask",dimX,dimY,dimZ,16,ij.gui.NewImage.FILL_WHITE);		
		VitimageUtils.adjustImageCalibration(out, img);
		int xMax=img.getWidth();
		for(int z=z0;z<z0+dimZ;z++) {
			short[] valsImg=(short[])img.getStack().getProcessor(z+1).getPixels();
			short[] valsOut=(short[])out.getStack().getProcessor(z-z0+1).getPixels();
			for(int x=x0;x<x0+dimX;x++) {
				for(int y=y0;y<y0+dimY;y++){
					valsOut[dimX*(y-y0)+(x-x0)]=((short)(valsImg[xMax*y+x] & 0xffff));
				}			
			}
		}
		return out;
	}
	
	
	public static ImagePlus cropImageCapillary(ImagePlus img,int x0,int y0,int z0,int rayX,int rayY,int rayZ) {
		if(img.getType()!=ImagePlus.GRAY16)return null;
		ImagePlus out=ij.gui.NewImage.createImage("Mask",rayX*2+1,rayY*2+1,rayZ*2+1,16,ij.gui.NewImage.FILL_WHITE);		
		VitimageUtils.adjustImageCalibration(out, img);
		int xMax=img.getWidth();
		int yMax=img.getHeight();
		int zMax=img.getStackSize();
		for(int z=-rayZ;z<=rayZ;z++) {
			int slice=z0+z;
			if(slice<0)slice=zMax+slice;
			if(slice>=zMax)slice=slice-zMax;
			short[] valsImg=(short[])img.getStack().getProcessor(slice+1).getPixels();
			short[] valsOut=(short[])out.getStack().getProcessor(z+rayZ+1).getPixels();
			for(int x=-rayX;x<=rayX;x++) {
				int xx=x+x0;
				if(xx<0)xx=xMax+xx;
				if(xx>=xMax)xx=xx-xMax;
				for(int y=-rayY;y<=rayY;y++) {
					int yy=y+y0;
					if(yy<0)yy=yMax+yy;
					if(yy>=yMax)yy=yy-yMax;
					valsOut[(rayX*2+1)*(y+rayY)+(x+rayX)]=((short)(valsImg[xMax*yy+xx] & 0xffff));
				}			
			}
		}
		return out;
	}


	public static ImagePlus uncropImageShort(ImagePlus img,int x0,int y0,int z0,int dimX,int dimY,int dimZ) {
		if(img.getType()!=ImagePlus.GRAY16) {			System.out.println("uncropImageShort : unsupported format : "+img.getType()+" format expected="+ImagePlus.GRAY16);return null;}
		int oldDimX=img.getWidth();
		int oldDimY=img.getHeight();
		int oldDimZ=img.getStackSize();
		
		ImagePlus out=ij.gui.NewImage.createImage("Mask",dimX,dimY,dimZ,16,ij.gui.NewImage.FILL_BLACK);		
		VitimageUtils.adjustImageCalibration(out, img);
		for(int z=z0;z<z0+oldDimZ;z++) {
			short[] valsImg=(short[])img.getStack().getProcessor(z-z0+1).getPixels();
			short[] valsOut=(short[])out.getStack().getProcessor(z+1).getPixels();
			for(int x=x0;x<x0+oldDimX;x++) {
				for(int y=y0;y<y0+oldDimY;y++){
					valsOut[dimX*(y)+(x)]=((short)(valsImg[oldDimX*(y-y0)+(x-x0)] & 0xffff));
				}			
			}
		}
		return out;
	}

	
	public static ImagePlus getSliceUncropped(ImagePlus img,int slice,int offsetX,int newDimX) {
		int oldDimX=img.getWidth();
		int dimY=img.getHeight();
		ImagePlus out=ij.gui.NewImage.createImage("out",newDimX,dimY,1,8,ij.gui.NewImage.FILL_BLACK);		
		VitimageUtils.adjustImageCalibration(out, img);
		byte[] valsImg=(byte[])img.getStack().getProcessor(slice).getPixels();
		byte[] valsOut=(byte[])out.getStack().getProcessor(1).getPixels();
		for(int x=offsetX;x<offsetX+oldDimX && x<newDimX;x++) {
			for(int y=0;y<dimY ;y++){
				valsOut[newDimX*(y)+(x)]=((byte)(valsImg[oldDimX*(y)+(x-offsetX)] & 0xff));
			}			
		}
		
		return out;
	}

	public static ImagePlus uncropImageByte(ImagePlus img,int x0,int y0,int z0,int dimX,int dimY,int dimZ) {
		if(img.getType()!=ImagePlus.GRAY8)return null;
		int oldDimX=img.getWidth();
		int oldDimY=img.getHeight();
		int oldDimZ=img.getStackSize();
		
		ImagePlus out=ij.gui.NewImage.createImage("Mask",dimX,dimY,dimZ,8,ij.gui.NewImage.FILL_BLACK);		
		VitimageUtils.adjustImageCalibration(out, img);
		for(int z=z0;z<z0+oldDimZ && z<z0+dimZ;z++) {
			byte[] valsImg=(byte[])img.getStack().getProcessor(z-z0+1).getPixels();
			byte[] valsOut=(byte[])out.getStack().getProcessor(z+1).getPixels();
			for(int x=x0;x<x0+oldDimX && x<x0+dimX;x++) {
				for(int y=y0;y<y0+oldDimY && y<y0+dimY;y++){
					valsOut[dimX*(y)+(x)]=((byte)(valsImg[oldDimX*(y-y0)+(x-x0)] & 0xff));
				}			
			}
		}
		return out;
	}
	

	
	public static ImagePlus cropImageByte(ImagePlus img,int x0,int y0,int z0,int dimX,int dimY,int dimZ) {
		if(img.getType()!=ImagePlus.GRAY8)return null;
		ImagePlus out=ij.gui.NewImage.createImage("Mask",dimX,dimY,dimZ,8,ij.gui.NewImage.FILL_WHITE);		
		VitimageUtils.adjustImageCalibration(out, img);
		int xMax=img.getWidth();
		for(int z=z0;z<z0+dimZ;z++) {
			byte[] valsImg=(byte[])img.getStack().getProcessor(z+1).getPixels();
			byte[] valsOut=(byte[])out.getStack().getProcessor(z-z0+1).getPixels();
			for(int x=x0;x<x0+dimX;x++) {
				for(int y=y0;y<y0+dimY;y++){
					valsOut[dimX*(y-y0)+(x-x0)]=((byte)(valsImg[xMax*y+x] & 0xff));
				}			
			}
		}
		return out;
	}
	

	
	public static ImagePlus cropImageFloat(ImagePlus img,int x0,int y0,int z0,int dimX,int dimY,int dimZ) {
		if(img.getType()!=ImagePlus.GRAY32)return null;
		ImagePlus out=ij.gui.NewImage.createImage("Mask",dimX,dimY,dimZ,32,ij.gui.NewImage.FILL_WHITE);		
		VitimageUtils.adjustImageCalibration(out, img);
		int xMax=img.getWidth();
		for(int z=z0;z<z0+dimZ;z++) {
			float[] valsImg=(float[])img.getStack().getProcessor(z+1).getPixels();
			float[] valsOut=(float[])out.getStack().getProcessor(z-z0+1).getPixels();
			for(int x=x0;x<x0+dimX;x++) {
				for(int y=y0;y<y0+dimY;y++){
					valsOut[dimX*(y-y0)+(x-x0)]=(float)valsImg[xMax*y+x];
				}			
			}
		}
		return out;
	}
	
	/*
	public static Point3d[] detectAxis(Acquisition acq,int delayForReacting){
		ImagePlus img=null;
		img=acq.getImageForRegistration();
		AcquisitionType acqType=acq.acquisitionType;
		boolean debug=false;
		int xMax=img.getWidth();
		int yMax=img.getHeight();
		int zMax=img.getStackSize();
		double vX=img.getCalibration().pixelWidth;
		double vY=img.getCalibration().pixelHeight;
		double vZ=img.getCalibration().pixelDepth;
		double xMoyUp=0,yMoyUp=0,zMoyUp=0;
		double xMoyDown=1,yMoyDown=1,zMoyDown=1;
		int hitsUp=0,hitsDown=0;
		if(debug)imageChecking(img,"Start detect axis type "+acq.getAcquisitionType());

		//Step 1 : apply gaussian filtering and convert to 8 bits
		if(acqType != AcquisitionType.RX)img=VitimageUtils.gaussianFiltering(img, 18*0.035 , 18*0.035 , 3*0.5);
		img.getProcessor().setMinAndMax(
				( acqType==AcquisitionType.MRI_T1_SEQ ? 200 : (acqType==AcquisitionType.MRI_GE3D ? 700 : 10000)), 
				( acqType==AcquisitionType.MRI_T1_SEQ ? 3000 : (acqType==AcquisitionType.MRI_GE3D ? 10000 : 50000)) );
		StackConverter sc=new StackConverter(img);
		sc.convertToGray8();
		if(debug)imageChecking(img,"fin step1 ");

		//Step 2 : apply automatic threshold
		ByteProcessor[] maskTab=new ByteProcessor[zMax];

		if(acqType == AcquisitionType.RX)img=VitimageUtils.eraseBorder(img);
		if(debug)imageChecking(img,"after Erase ");
		if(acqType != AcquisitionType.MRI_T2_SEQ) {
			
			System.out.println("Mask lookup for center of object, case of hign SNR (T1 or RX)");
			for(int z=0;z<zMax;z++){
				maskTab[z]=(ByteProcessor) img.getStack().getProcessor(z+1);
				maskTab[z].setAutoThreshold("Otsu dark");
				maskTab[z]=maskTab[z].createMask();
			}
		}
		else {
			System.out.println("Mask lookup for center of object, case of low SNR (T2)");
			for(int z=0;z<zMax;z++){
				maskTab[z]=(ByteProcessor) img.getStack().getProcessor(z+1);
				maskTab[z].setThreshold(20,255,1);
				maskTab[z]=maskTab[z].createMask();
			}
			
		}
			
			
		//Step 2.1 : Extract two substacks for the upper part and the lower part of the object
		ImageStack stackUp = new ImageStack(xMax, yMax);	
		ImageStack stackDown = new ImageStack(xMax, yMax);
		int zQuarter=zMax/4;
		int zVentile=zMax/40;
		zVentile=(zVentile < 10 ? 10 : zVentile);
		if(zMax<zVentile*2+2)zVentile=zMax/2-1;
		for(int i=0;i<zVentile;i++) {
			stackUp.addSlice("",maskTab[zMax/2+zQuarter-zVentile+i] );//de zmax/2 +zQuarter-zVentile à zMax/2 + zQuarter-zVentile5 --> ajouter zMax/2 à la fin			
			stackDown.addSlice("",maskTab[zMax/2-zQuarter+i+1] );//de zmax/2-5 à zMax/2   --> ajouter zMax/2-5 à la fin
		}
		ImagePlus imgUp=new ImagePlus("upMASK",stackUp);
		VitimageUtils.adjustImageCalibration(imgUp, img);
		if(debug)imageChecking(imgUp);				
		ImagePlus imgUpCon=connexe(imgUp,0,29,0,10E10,6,2,true);
		if(debug)imageChecking(imgUpCon,"imgUpCon");
		
		ImagePlus imgDown=new ImagePlus("downMASK",stackDown);
		VitimageUtils.adjustImageCalibration(imgDown, img);
		if(debug)imageChecking(imgDown);
		ImagePlus imgDownCon=connexe(imgDown,0,29,0,10E10,6,2,true);
		if(debug)imageChecking(imgDownCon,"imgDownCon");
		IJ.saveAsTiff(imgUpCon,"/home/fernandr/Bureau/pouet.tif");
		
		if(VitimageUtils.isNullImage(imgUpCon)) {
			System.out.println("Handling case of void moelle");
			if(debug)imageChecking(imgUp,"Up init");
			imgUpCon=VitimageUtils.gaussianFiltering(imgUp, 30*vX, 30*vY, 3*vZ);
			if(debug)imageChecking(imgUpCon,"Apres filtrage");
			//imgUpCon.show();
			//VitimageUtils.waitFor(10000);
			imgUpCon=VitimageUtils.getBinaryMask(imgUpCon, 253);
			if(debug)imageChecking(imgUpCon,"Apres seuillage");
			imgUpCon=connexe(imgUpCon,30,256,0,10E10,6,1,true);
			if(debug)imageChecking(imgUpCon,"Apres connexe");
		}

		if(VitimageUtils.isNullImage(imgDownCon)) {
			System.out.println("Handling case of void moelle");
			if(debug)imageChecking(imgDown,"Down init");
			imgDownCon=VitimageUtils.gaussianFiltering(imgDown, 30*vX, 30*vY, 3*vZ);
			if(debug)imageChecking(imgDownCon,"Apres filtrage");
			imgDownCon=VitimageUtils.getBinaryMask(imgDownCon, 254);
			if(debug)imageChecking(imgDownCon,"Apres seuillage");
			imgDownCon=connexe(imgDownCon,30,256,0,10E10,6,1,true);
			if(debug)imageChecking(imgDownCon,"Apres connexe");
		}
		
		
		
		//Step 3 : compute the two centers of mass
		short[][]valsDownCon=new short[zQuarter][];
		short[][]valsUpCon=new short[zQuarter][];
		for(int z=0;z<zVentile;z++){
			valsDownCon[z]=(short[])(imgDownCon).getStack().getProcessor(z+1).getPixels();
			valsUpCon[z]=(short[])(imgUpCon).getStack().getProcessor(z+1).getPixels();
		}

		for(int x=0;x<xMax;x++){
			for(int y=0;y<yMax;y++){
				for(int z=0;z<zVentile;z++){								
					if(valsDownCon[z][xMax*y+x]==((short)255)){//We are in the first part of the object
						hitsDown++;
						xMoyDown+=x;yMoyDown+=y;zMoyDown+=z;
					}
					if(valsUpCon[z][xMax*y+x]==((short	)255)){//We are in the first part of the object
						hitsUp++;
						xMoyUp+=x;yMoyUp+=y;zMoyUp+=z;
					}
				}
			}
		}
		if(hitsUp==0)hitsUp=1;
		if(hitsDown==0)hitsDown=1;
		xMoyUp=xMoyUp/hitsUp;//Center of mass computation. 
		yMoyUp=yMoyUp/hitsUp;//Double type stands a 15 digits precisions; which is enough here, until #voxels < 5.10^12 
		zMoyUp=zMoyUp/hitsUp+zMax/2+zQuarter-zVentile;//due to the extraction of a substack zmax/2-zQuarter+1 - zmax/2     zMax/2+zQuarter-zVentile

		xMoyDown=xMoyDown/hitsDown;//Center of mass computation. 
		yMoyDown=yMoyDown/hitsDown;//Double type stands a 15 digits precisions; which is enough here, until #voxels < 5.10^12 
		zMoyDown=zMoyDown/hitsDown+zMax/2-zQuarter+1;//due to the extraction of a substack zmax/2 - zmax/2+zQuarter       zMax/2-zQuarter+1

		if(debug) {
			System.out.println("HitsUp="+hitsUp+" ..Center of mass up = "+xMoyUp+"  ,  "+yMoyUp+"  ,  "+zMoyUp);
			System.out.println("HitsDown="+hitsDown+" ..Center of mass down = "+xMoyDown+"  ,  "+yMoyDown+"  ,  "+zMoyDown);
		}

		xMoyUp=xMoyUp*vX;		
		yMoyUp=yMoyUp*vY;		
		zMoyUp=zMoyUp*vZ;	
		xMoyDown=xMoyDown*vX;		
		yMoyDown=yMoyDown*vY;		
		zMoyDown=zMoyDown*vZ;		
		if(debug) {
			System.out.println("Center of mass up (coord reel)= "+xMoyUp+"  ,  "+yMoyUp+"  ,  "+zMoyUp);
			System.out.println("Center of mass down (coord reel)= "+xMoyDown+"  ,  "+yMoyDown+"  ,  "+zMoyDown);
		}

		//Step 4 : compute the axis vector, that will stands for Z vector after alignement
		double[]vectZ=TransformUtils.normalize(new double[] {xMoyUp - xMoyDown , yMoyUp - yMoyDown , zMoyUp - zMoyDown});
		double[][]axisVerificationMatrix=VitiDialogs.inspectAxis( acq.getTransformedRegistrationImage() ,vectZ,new Point3d(xMoyUp*0.5+xMoyDown*0.5 , yMoyUp*0.5+yMoyDown*0.5 , zMoyUp*0.5+zMoyDown*0.5 ),delayForReacting);
		double []vectZbis=axisVerificationMatrix[0];
		double epsilon=0.0000001;
		if(TransformUtils.norm(TransformUtils.vectorialSubstraction(vectZbis,vectZ))>epsilon) {//Une erreur a été corrigée par l'utilisateur
			System.out.println("En effet il y a eu modification");
			xMoyUp=axisVerificationMatrix[1][0];xMoyDown=axisVerificationMatrix[2][0];
			yMoyUp=axisVerificationMatrix[1][1];yMoyDown=axisVerificationMatrix[2][1];
			zMoyUp=axisVerificationMatrix[1][2];zMoyDown=axisVerificationMatrix[2][2];
			vectZ=vectZbis;
		}

		
		System.out.println("Vecteur axial ="+TransformUtils.stringVector(vectZ,""));
		double []vectXtmp=new double[] {1,0,0};
		double []vectX=TransformUtils.normalize(TransformUtils.vectorialSubstraction(vectXtmp, TransformUtils.proj_u_of_v(vectZ, vectXtmp)));
		System.out.println("Vecteur orthogonal ="+TransformUtils.stringVector(vectX,""));

		
		
		

		//Step 5 : compute the three points
		Point3d origine=new Point3d(xMoyUp*0.5+xMoyDown*0.5 , yMoyUp*0.5+yMoyDown*0.5 , zMoyUp*0.5+zMoyDown*0.5 );
		Point3d ptUp= new Point3d(origine.x + vectZ[0]   ,  origine.y + vectZ[1] , origine.z + vectZ[2]);
		Point3d ptRight= new Point3d(origine.x + vectX[0]   ,  origine.y + vectX[1] , origine.z + vectX[2]);
		return new Point3d[] {origine,ptUp,ptRight};
	}
	*/
	
	
	
	
	public static double[][] getCentersOfSlicesFromMask(ImagePlus img){
		img.show();
		Analyzer an=new Analyzer(img);
		int zMax=img.getStackSize();
		double voxZ=VitimageUtils.getVoxelSizes(img)[2];
		double[][]tabRet=new double[zMax][3];
		for(int i=0;i<zMax;i++) {
			img.setSlice(i+1);
			IJ.run(img, "Select All", "");
			an.measure();
			an.displayResults();
			tabRet[i][0]=an.getResultsTable().getValue(8,i);
			tabRet[i][1]=an.getResultsTable().getValue(9,i);
			tabRet[i][2]=i*voxZ;
		}
		img.hide();
		return tabRet;
	}

	
	
	
	
	/*
	public static ImagePlus smoothContourOfPlant(ImagePlus img2,int slice) {
		int zMax=img2.getStackSize();
		double vX=img2.getCalibration().pixelWidth;
		double []val=Acquisition.caracterizeBackgroundOfImage(img2);
		double mu=val[0];
		double sigma=val[1];
		double thresh=mu+3*sigma;
//		VitimageUtils.imageChecking(img2,"A l arrivee de smooth contour");
		ImagePlus img=VitimageUtils.gaussianFiltering(img2,vX*0.5,vX*0.5,vX*0.5);
		img.getStack().getProcessor(1).set(0);
		img.getStack().getProcessor(2).set(0);
		img.getStack().getProcessor(3).set(0);
		img.getStack().getProcessor(4).set(0);
		img.getStack().getProcessor(zMax).set(0);
		img.getStack().getProcessor(zMax-1).set(0);
		img.getStack().getProcessor(zMax-2).set(0);
		img.getStack().getProcessor(zMax-3).set(0);
		System.out.println("Mu fond="+mu+" , Sigma fond="+sigma+" , Thresh="+thresh );
		ImagePlus imgMask=VitimageUtils.getBinaryMask(img,thresh);
		//						VitimageUtils.imageChecking(imgMask,"Premier masque");
		imgMask=VitimageUtils.connexe(imgMask,1,256, 0,10E10, 6, 1,false);//L objet
		imgMask=new Duplicator().run(imgMask,slice,slice);
		//				VitimageUtils.imageChecking(imgMask,"Connexe");
		IJ.run(imgMask,"8-bit","");
		IJ.run(imgMask,"Dilate","");
		IJ.run(imgMask,"Dilate","");
		IJ.run(imgMask,"Dilate","");
		IJ.run(imgMask,"Dilate","");
		IJ.run(imgMask,"Dilate","");
		IJ.run(imgMask,"Dilate","");
		IJ.run(imgMask,"Dilate","");
		IJ.run(imgMask,"Dilate","");
		IJ.run(imgMask,"Erode","");
		IJ.run(imgMask,"Erode","");
		IJ.run(imgMask,"Erode","");
		IJ.run(imgMask,"Erode","");
		IJ.run(imgMask,"Erode","");
		IJ.run(imgMask,"Erode","");
		IJ.run(imgMask,"Erode","");
		IJ.run(imgMask,"Erode","");
//		VitimageUtils.imageChecking(imgMask,"8bit");
//		VitimageUtils.imageChecking(imgMask,"Apres morpho");
		
		IJ.run(imgMask, "Fill Holes", "stack");
		//		VitimageUtils.imageChecking(imgMask,"Apres fill holes");
		
		imgMask=VitimageUtils.gaussianFiltering(imgMask, 10*vX, 10*vX,0);
		//		VitimageUtils.imageChecking(imgMask,"Apres lissage");
		imgMask.getProcessor().resetMinAndMax();
		return VitimageUtils.getBinaryMask(imgMask, 210);		
	}
	*/
	
	public static Point3d[] detectInoculationPointManually(ImagePlus img, Point3d inocPoint) {
		boolean ghetto=true;
		
		Point3d origine=TransformUtils.convertPointToRealSpace(new Point3d( img.getWidth()/2.0 , img.getHeight()/2.0 ,0),img);
		origine.z=inocPoint.z;

		double[]vect=new double[] {inocPoint.x - origine.x , inocPoint.y - origine.y , inocPoint.z - origine.z };
		double[]vectNorm=TransformUtils.normalize(vect);
		System.out.println(TransformUtils.stringVector(vectNorm,"Vecteur normalisé de la moelle vers le point d inoculation"));
		Point3d originePlusDinoc=new Point3d( origine.x + vectNorm[0] , origine.y + vectNorm[1] , inocPoint.z  + vectNorm[2]);
		Point3d originePlusDz=new Point3d( origine.x  ,origine.y , origine.z + 1 );
		Point3d[]ret=new Point3d[] { origine ,  originePlusDz , originePlusDinoc , inocPoint};
		return ret;		
	}

	public static Point3d[] detectInoculationPointGuidedByAutomaticComputedOutline(ImagePlus img,ImagePlus maskForOutline) {
		boolean ghetto=true;
		ImagePlus imgCheck=new Duplicator().run(img);
		int xMax=img.getWidth();
		int yMax=img.getHeight();
		int zMax=img.getStackSize();
		double vX=img.getCalibration().pixelWidth;
		double vY=img.getCalibration().pixelHeight;
		double vZ=img.getCalibration().pixelDepth;
		int facteurAniso=(int)Math.round(vZ/vX);
		double IPStdZSize=4; //mm
		double IPStdXSize=2; //mm
		double sigmaXY=IPStdXSize/2.0;
		double sigmaZ=IPStdXSize/4.0;
		int sigmaPlotZ=5;
		double sigmaXYInPixels=10*0.035/vX;//sigmaXY/vX;
		double sigmaZInPixels=0.2*0.5/vZ;//sigmaZ/vZ;
		int minPossibleZ=zMax/4+2;
		int maxPossibleZ=(zMax*3)/4-2;
		ImagePlus imgSlice=new Duplicator().run(maskForOutline,1,1);
		IJ.run(imgSlice, "Outline", "stack");
		imgSlice.show();
		VitimageUtils.waitFor(1000);
		imgSlice.hide();
		ImagePlus imgOutline= new Duplicator().run(imgSlice, 1,1);
		imgSlice=connexe(imgSlice,255,256,0,10E10,8,1,true);
		System.out.println(" Ok.");


		System.out.print("Selection equipartited points for analysis and sort by angle around the center...");
		IJ.run(imgSlice, "8-bit", "");
		imgSlice.getProcessor().invert();
		RoiManager rm=RoiManager.getRoiManager();
		rm.reset();
		IJ.run(imgSlice, "Create Selection", "");
		rm.addRoi(imgSlice.getRoi());
		Roi roi=rm.getRoi(0);
		FloatPolygon fp=roi.getContainedFloatPoints();
		int nAngles=fp.npoints;
		System.out.println("Nombre de points selectionnes : "+nAngles);
		double [][]tabCoord=new double[nAngles][3];
		Double [][]tabSort=new Double[nAngles][3];
		double xCenter = img.getWidth()/2;
		double yCenter = img.getHeight()/2;
		for (int i=0; i<nAngles; i++) {
			tabSort[i][0]=new Double(fp.xpoints[i]);
			tabSort[i][1]=new Double(fp.ypoints[i]);
			tabSort[i][2]=new Double(TransformUtils.calculateAngle(tabSort[i][0]-xCenter,yCenter-tabSort[i][1]));
			
		}
		imgSlice.changes=false;
		imgSlice.close();
		rm.close();
		//sort by angles
		Arrays.sort(tabSort,new AngleComparator());
		for (int i=0; i<nAngles; i++) {
			tabCoord[i][0]=tabSort[i][0].doubleValue();
			tabCoord[i][1]=tabSort[i][1].doubleValue();
			tabCoord[i][2]=tabSort[i][2].doubleValue();
		}
		System.out.println(" Ok.");
		double [][]meanValues=new double[nAngles][zMax];

		System.out.print("Measurements");
		ImagePlus measures=ij.gui.NewImage.createImage("measures",nAngles,zMax*facteurAniso,3,32,ij.gui.NewImage.FILL_BLACK);
		float[]measuresImg0=(float[]) measures.getStack().getProcessor(1).getPixels();
		float[]measuresImg1=(float[]) measures.getStack().getProcessor(2).getPixels();
		float[]measuresImg2=(float[]) measures.getStack().getProcessor(3).getPixels();

		for (int ang=0; ang<nAngles; ang++){
			for (int z=0; z<zMax; z++){			
				meanValues[ang][z]=VitimageUtils.meanValueofImageAround(img,(int)Math.round(tabCoord[ang][0]),(int)Math.round(tabCoord[ang][1]),z,sigmaXYInPixels);
				for(int i=0;i<facteurAniso;i++)measuresImg0[nAngles*(z*facteurAniso+i)+ang]=(float) meanValues[ang][z];
			}
		}

		System.out.println(" Ok.");




		System.out.print("Score computation");
		//
		double[][][]scores=new double[nAngles][zMax][3];
		for (int ang=0; ang<nAngles; ang++){
			for (int z=minPossibleZ; z<=maxPossibleZ; z++){			
				double acc=0;
				for(int i=-sigmaPlotZ;i<=sigmaPlotZ;i++)acc+=meanValues[ang][z+i];
				scores[ang][z][0]=acc/(2*sigmaPlotZ+1);
				scores[ang][z][1]=(scores[ang][z][0]-meanValues[ang][z])/scores[ang][z][0];
				scores[ang][z][2]=(scores[ang][z][0]-meanValues[ang][z]);
				for(int i=0;i<facteurAniso;i++)measuresImg1[nAngles*(z*facteurAniso+i)+ang]=(float) scores[ang][z][1];
				for(int i=0;i<facteurAniso;i++)measuresImg2[nAngles*(z*facteurAniso+i)+ang]=(float) scores[ang][z][2];
			}
		}

		System.out.println(" Ok.");
		measures.getProcessor().setMinAndMax(measures.getProcessor().getMin(),measures.getProcessor().getMax());
		IJ.run(measures,"Fire","");
		//	anna.storeImage(measures, "Image de score");
		ImagePlus imgDetect=new Duplicator().run(measures,1,measures.getStackSize());
		IJ.run(imgDetect, "Gaussian Blur...", "sigma="+(1/(2*vX))+" stack");
		//	anna.remember("Parametre de lissage utilise, en pixel","sigma="+(1/(2*vX)));
		//	anna.storeImage(imgDetect, "Image de score lissee");
		imgDetect.getProcessor().resetMinAndMax();
		imgDetect.show();
		imgDetect.setTitle("Score map for inoculation detection");
		VitimageUtils.waitFor(4000);
		imgDetect.hide();
		double[][]coordMax=TransformUtils.getCoordinatesOf(imgDetect,VitimageUtils.COORD_OF_MAX_IN_TWO_LAST_SLICES,minPossibleZ*facteurAniso,maxPossibleZ*facteurAniso);
		System.out.println("Maximum relatif obtenu à ("+coordMax[0][0]+" , "+coordMax[0][1]+" ) soit, en coordonnees images : ( "+
				tabCoord[(int)Math.round(coordMax[0][0])][0]+" , "+tabCoord[(int)Math.round(coordMax[0][0])][1]+" , "+
				((coordMax[0][1]-facteurAniso/2.0)/facteurAniso)+" )");
		System.out.println("Maximum absolu obtenu à ("+coordMax[1][0]+" , "+coordMax[1][1]+" ) soit, en coordonnees images : ( "+
				tabCoord[(int)Math.round(coordMax[1][0])][0]+" , "+tabCoord[(int)Math.round(coordMax[1][0])][1]+" , "+
				((coordMax[1][1]-facteurAniso/2.0)/facteurAniso)+" )");
		Point3d inocPoint=TransformUtils.convertPointToRealSpace(new Point3d( tabCoord[(int)Math.round(coordMax[1][0])][0],  tabCoord[(int)Math.round(coordMax[1][0])][1],  ((coordMax[1][1]-facteurAniso/2.0)/facteurAniso) ),img) ;

		inocPoint=VitiDialogs.inspectInoculationPoint( imgCheck ,inocPoint);

		
		
		Point3d origine=TransformUtils.convertPointToRealSpace(new Point3d( xMax/2.0 , yMax/2.0 ,0),img);
		origine.z=inocPoint.z;

		double[]vect=new double[] {inocPoint.x - origine.x , inocPoint.y - origine.y , inocPoint.z - origine.z };
		double[]vectNorm=TransformUtils.normalize(vect);
		System.out.println(TransformUtils.stringVector(vectNorm,"Vecteur normalisé de la moelle vers le point d inoculation"));
		Point3d originePlusDinoc=new Point3d( origine.x + vectNorm[0] , origine.y + vectNorm[1] , inocPoint.z  + vectNorm[2]);
		Point3d originePlusDz=new Point3d( origine.x  ,origine.y , origine.z + 1 );
		Point3d[]ret=new Point3d[] { origine ,  originePlusDz , originePlusDinoc , inocPoint};
		imgCheck.close();
		return ret;		
	}

/*
	public static Point3d[] detectInoculationPoint(ImagePlus img,double thresholdMin) {
		ImagePlus imgCheck=new Duplicator().run(img);
		int xMax=img.getWidth();
		int yMax=img.getHeight();
		int zMax=img.getStackSize();
		double vX=img.getCalibration().pixelWidth;
		double vY=img.getCalibration().pixelHeight;
		double vZ=img.getCalibration().pixelDepth;
		int facteurAniso=(int)Math.round(vZ/vX);
		double IPStdZSize=4; //mm
		double IPStdXSize=2; //mm
		double sigmaXY=IPStdXSize/2.0;
		double sigmaZ=IPStdXSize/4.0;
		int sigmaPlotZ=5;
		double sigmaXYInPixels=10*0.035/vX;//sigmaXY/vX;
		double sigmaZInPixels=0.2*0.5/vZ;//sigmaZ/vZ;
		int minPossibleZ=zMax/4+2;
		int maxPossibleZ=(zMax*3)/4-2;
		System.out.print("Blur");
		IJ.run(img, "Gaussian Blur 3D...", "x="+sigmaXYInPixels+" y="+sigmaXYInPixels+" z="+sigmaZInPixels+"");
		System.out.println(" Ok.");
		imageChecking(img);
		ImagePlus imgSlice= new Duplicator().run(img, minPossibleZ,minPossibleZ);
		System.out.print("Outline detection ...");
		imgSlice.getProcessor().resetMinAndMax();
		imgSlice.show();
		VitimageUtils.waitFor(2000);
		imgSlice.hide();
			System.out.println("Application seuillage de valeur "+thresholdMin);
		imgSlice.getProcessor().setThreshold(thresholdMin,Math.pow(2,16)-1,ImageProcessor.BLACK_AND_WHITE_LUT);
		Prefs.blackBackground = true;
		IJ.run(imgSlice, "Convert to Mask", "method=Default background=Dark calculate black");
		VitimageUtils.waitFor(2000);
		for(int er=0;er<6;er++) {
			IJ.run(imgSlice, "Erode", "stack");
			imgSlice.setTitle("Erosion numero "+er);
			imgSlice.show();
			VitimageUtils.waitFor(2000);
			imgSlice.hide();
		}
		imgSlice.show();
		VitimageUtils.waitFor(1000);
		imgSlice.hide();
		IJ.run(imgSlice, "Outline", "stack");
		imgSlice.show();
		imgSlice.show();
		VitimageUtils.waitFor(1000);
		imgSlice.hide();
		ImagePlus imgOutline= new Duplicator().run(imgSlice, 1,1);
		imgSlice=connexe(imgSlice,255,256,0,10E10,8,1,true);
		System.out.println(" Ok.");


		System.out.print("Selection equipartited points for analysis and sort by angle around the center...");
		IJ.run(imgSlice, "8-bit", "");
		imgSlice.show();
		VitimageUtils.waitFor(1000);
		imgSlice.hide();
		imgSlice.getProcessor().invert();
		RoiManager rm=RoiManager.getRoiManager();
		rm.reset();
		IJ.run(imgSlice, "Create Selection", "");
		rm.addRoi(imgSlice.getRoi());
		Roi roi=rm.getRoi(0);
		FloatPolygon fp=roi.getContainedFloatPoints();
		int nAngles=fp.npoints;
		System.out.println("Nombre de points selectionnes : "+nAngles);
		double [][]tabCoord=new double[nAngles][3];
		Double [][]tabSort=new Double[nAngles][3];
		double xCenter = img.getWidth()/2;
		double yCenter = img.getHeight()/2;
		for (int i=0; i<nAngles; i++) {
			tabSort[i][0]=new Double(fp.xpoints[i]);
			tabSort[i][1]=new Double(fp.ypoints[i]);
			tabSort[i][2]=new Double(TransformUtils.calculateAngle(tabSort[i][0]-xCenter,yCenter-tabSort[i][1]));
			
		}
		imgSlice.changes=false;
		imgSlice.close();
		rm.close();
		//sort by angles
		Arrays.sort(tabSort,new AngleComparator());
		for (int i=0; i<nAngles; i++) {
			tabCoord[i][0]=tabSort[i][0].doubleValue();
			tabCoord[i][1]=tabSort[i][1].doubleValue();
			tabCoord[i][2]=tabSort[i][2].doubleValue();
		}
		System.out.println(" Ok.");
		double [][]meanValues=new double[nAngles][zMax];

		System.out.print("Measurements");
		ImagePlus measures=ij.gui.NewImage.createImage("measures",nAngles,zMax*facteurAniso,3,32,ij.gui.NewImage.FILL_BLACK);
		float[]measuresImg0=(float[]) measures.getStack().getProcessor(1).getPixels();
		float[]measuresImg1=(float[]) measures.getStack().getProcessor(2).getPixels();
		float[]measuresImg2=(float[]) measures.getStack().getProcessor(3).getPixels();

		for (int ang=0; ang<nAngles; ang++){
			for (int z=0; z<zMax; z++){			
				meanValues[ang][z]=VitimageUtils.meanValueofImageAround(img,(int)Math.round(tabCoord[ang][0]),(int)Math.round(tabCoord[ang][1]),z,sigmaXYInPixels);
				for(int i=0;i<facteurAniso;i++)measuresImg0[nAngles*(z*facteurAniso+i)+ang]=(float) meanValues[ang][z];
			}
		}

		System.out.println(" Ok.");




		System.out.print("Score computation");
		//
		double[][][]scores=new double[nAngles][zMax][3];
		for (int ang=0; ang<nAngles; ang++){
			for (int z=minPossibleZ; z<=maxPossibleZ; z++){			
				double acc=0;
				for(int i=-sigmaPlotZ;i<=sigmaPlotZ;i++)acc+=meanValues[ang][z+i];
				scores[ang][z][0]=acc/(2*sigmaPlotZ+1);
				scores[ang][z][1]=(scores[ang][z][0]-meanValues[ang][z])/scores[ang][z][0];
				scores[ang][z][2]=(scores[ang][z][0]-meanValues[ang][z]);
				for(int i=0;i<facteurAniso;i++)measuresImg1[nAngles*(z*facteurAniso+i)+ang]=(float) scores[ang][z][1];
				for(int i=0;i<facteurAniso;i++)measuresImg2[nAngles*(z*facteurAniso+i)+ang]=(float) scores[ang][z][2];
			}
		}

		System.out.println(" Ok.");
		measures.getProcessor().setMinAndMax(measures.getProcessor().getMin(),measures.getProcessor().getMax());
		IJ.run(measures,"Fire","");
		//	anna.storeImage(measures, "Image de score");
		ImagePlus imgDetect=new Duplicator().run(measures,1,measures.getStackSize());
		IJ.run(imgDetect, "Gaussian Blur...", "sigma="+(1/(2*vX))+" stack");
		//	anna.remember("Parametre de lissage utilise, en pixel","sigma="+(1/(2*vX)));
		//	anna.storeImage(imgDetect, "Image de score lissee");
		imgDetect.getProcessor().resetMinAndMax();
		imgDetect.show();
		imgDetect.setTitle("Score map for inoculation detection");
		VitimageUtils.waitFor(5000);
		imgDetect.hide();
		double[][]coordMax=TransformUtils.getCoordinatesOf(imgDetect,VitimageUtils.COORD_OF_MAX_IN_TWO_LAST_SLICES,minPossibleZ*facteurAniso,maxPossibleZ*facteurAniso);
		System.out.println("Maximum relatif obtenu à ("+coordMax[0][0]+" , "+coordMax[0][1]+" ) soit, en coordonnees images : ( "+
				tabCoord[(int)Math.round(coordMax[0][0])][0]+" , "+tabCoord[(int)Math.round(coordMax[0][0])][1]+" , "+
				((coordMax[0][1]-facteurAniso/2.0)/facteurAniso)+" )");
		System.out.println("Maximum absolu obtenu à ("+coordMax[1][0]+" , "+coordMax[1][1]+" ) soit, en coordonnees images : ( "+
				tabCoord[(int)Math.round(coordMax[1][0])][0]+" , "+tabCoord[(int)Math.round(coordMax[1][0])][1]+" , "+
				((coordMax[1][1]-facteurAniso/2.0)/facteurAniso)+" )");
		Point3d inocPoint=TransformUtils.convertPointToRealSpace(new Point3d( tabCoord[(int)Math.round(coordMax[1][0])][0],  tabCoord[(int)Math.round(coordMax[1][0])][1],  ((coordMax[1][1]-facteurAniso/2.0)/facteurAniso) ),img) ;

		inocPoint=VitiDialogs.inspectInoculationPoint( imgCheck ,inocPoint);

		
		
		Point3d origine=TransformUtils.convertPointToRealSpace(new Point3d( xMax/2.0 , yMax/2.0 ,0),img);
		origine.z=inocPoint.z;

		double[]vect=new double[] {inocPoint.x - origine.x , inocPoint.y - origine.y , inocPoint.z - origine.z };
		double[]vectNorm=TransformUtils.normalize(vect);
		System.out.println(TransformUtils.stringVector(vectNorm,"Vecteur normalisé de la moelle vers le point d inoculation"));
		Point3d originePlusDinoc=new Point3d( origine.x + vectNorm[0] , origine.y + vectNorm[1] , inocPoint.z  + vectNorm[2]);
		Point3d originePlusDz=new Point3d( origine.x  ,origine.y , origine.z + 1 );
		Point3d[]ret=new Point3d[] { origine ,  originePlusDz , originePlusDinoc , inocPoint};
		imgCheck.close();
		return ret;		
	}

	public static Point3d[] detectInoculationPointIRMT2(ImagePlus img,ImagePlus mask) {
		ImagePlus imgCheck=new Duplicator().run(img);
		int xMax=img.getWidth();
		int yMax=img.getHeight();
		int zMax=img.getStackSize();
		double vX=img.getCalibration().pixelWidth;
		double vY=img.getCalibration().pixelHeight;
		double vZ=img.getCalibration().pixelDepth;

		//Preparation des donnees pour lecture valeurs
		int facteurAniso=(int)Math.round(vZ/vX);
		double IPStdZSize=4; //mm
		double IPStdXSize=2; //mm
		double sigmaXY=IPStdXSize/2.0;
		double sigmaZ=IPStdXSize/4.0;
		int sigmaPlotZ=5;
		double sigmaXYInPixels=10*0.035/vX;//sigmaXY/vX;
		double sigmaZInPixels=0.2*0.5/vZ;//sigmaZ/vZ;
		int minPossibleZ=zMax/4+2;
		int maxPossibleZ=(zMax*3)/4-2;
		System.out.print("Blur");
		IJ.run(img, "Gaussian Blur 3D...", "x="+sigmaXYInPixels+" y="+sigmaXYInPixels+" z="+sigmaZInPixels+"");
		System.out.println(" Ok.");

		
		//Preparation masque outline
		
		
		IJ.run(mask, "Erode", "stack");
		IJ.run(mask, "Gaussian Blur 3D...", "x="+sigmaXYInPixels+" y="+sigmaXYInPixels+" z="+sigmaZInPixels+"");
		System.out.println("Application seuillage de valeur "+200);
		mask.getProcessor().setThreshold(200,255,ImageProcessor.BLACK_AND_WHITE_LUT);
		Prefs.blackBackground = true;
		IJ.run(mask, "Convert to Mask", "method=Default background=Dark calculate black");		
		ImagePlus imgSlice= new Duplicator().run(mask, (minPossibleZ+maxPossibleZ)/2,(minPossibleZ+maxPossibleZ)/2);
		System.out.print("Outline detection ...");
		imgSlice.show();
		VitimageUtils.waitFor(2000);
		imgSlice.hide();
		IJ.run(imgSlice, "Outline", "stack");
		imgSlice.show();
		VitimageUtils.waitFor(2000);
		imgSlice.hide();

		System.out.print("Selection equipartited points for analysis and sort by angle around the center...");
		IJ.run(imgSlice, "8-bit", "");
		imgSlice.show();
		VitimageUtils.waitFor(1000);
		imgSlice.hide();
		imgSlice.getProcessor().invert();
		RoiManager rm=RoiManager.getRoiManager();
		rm.reset();
		IJ.run(imgSlice, "Create Selection", "");
		rm.addRoi(imgSlice.getRoi());
		Roi roi=rm.getRoi(0);
		FloatPolygon fp=roi.getContainedFloatPoints();
		int nAngles=fp.npoints;
		System.out.println("Nombre de points selectionnes : "+nAngles);
		double [][]tabCoord=new double[nAngles][3];
		Double [][]tabSort=new Double[nAngles][3];
		double xCenter = img.getWidth()/2;
		double yCenter = img.getHeight()/2;
		for (int i=0; i<nAngles; i++) {
			tabSort[i][0]=new Double(fp.xpoints[i]);
			tabSort[i][1]=new Double(fp.ypoints[i]);
			tabSort[i][2]=new Double(TransformUtils.calculateAngle(tabSort[i][0]-xCenter,yCenter-tabSort[i][1]));
			
		}
		imgSlice.changes=false;
		imgSlice.close();
		rm.close();
		//sort by angles
		Arrays.sort(tabSort,new AngleComparator());
		for (int i=0; i<nAngles; i++) {
			tabCoord[i][0]=tabSort[i][0].doubleValue();
			tabCoord[i][1]=tabSort[i][1].doubleValue();
			tabCoord[i][2]=tabSort[i][2].doubleValue();
		}
		System.out.println(" Ok.");
		double [][]meanValues=new double[nAngles][zMax];

		System.out.print("Measurements");
		ImagePlus measures=ij.gui.NewImage.createImage("measures",nAngles,zMax*facteurAniso,3,32,ij.gui.NewImage.FILL_BLACK);
		float[]measuresImg0=(float[]) measures.getStack().getProcessor(1).getPixels();
		float[]measuresImg1=(float[]) measures.getStack().getProcessor(2).getPixels();
		float[]measuresImg2=(float[]) measures.getStack().getProcessor(3).getPixels();

		for (int ang=0; ang<nAngles; ang++){
			for (int z=0; z<zMax; z++){			
				meanValues[ang][z]=VitimageUtils.meanValueofImageAround(img,(int)Math.round(tabCoord[ang][0]),(int)Math.round(tabCoord[ang][1]),z,sigmaXYInPixels);
				for(int i=0;i<facteurAniso;i++)measuresImg0[nAngles*(z*facteurAniso+i)+ang]=(float) meanValues[ang][z];
			}
		}

		System.out.println(" Ok.");




		System.out.print("Score computation");
		//
		double[][][]scores=new double[nAngles][zMax][3];
		for (int ang=0; ang<nAngles; ang++){
			for (int z=minPossibleZ; z<=maxPossibleZ; z++){			
				double acc=0;
				for(int i=-sigmaPlotZ;i<=sigmaPlotZ;i++)acc+=meanValues[ang][z+i];
				scores[ang][z][0]=acc/(2*sigmaPlotZ+1);
				scores[ang][z][1]=(scores[ang][z][0]-meanValues[ang][z])/scores[ang][z][0];
				scores[ang][z][2]=(scores[ang][z][0]-meanValues[ang][z]);
				for(int i=0;i<facteurAniso;i++)measuresImg1[nAngles*(z*facteurAniso+i)+ang]=(float) scores[ang][z][1];
				for(int i=0;i<facteurAniso;i++)measuresImg2[nAngles*(z*facteurAniso+i)+ang]=(float) scores[ang][z][2];
			}
		}

		System.out.println(" Ok.");
		measures.getProcessor().setMinAndMax(measures.getProcessor().getMin(),measures.getProcessor().getMax());
		IJ.run(measures,"Fire","");
		//	anna.storeImage(measures, "Image de score");
		ImagePlus imgDetect=new Duplicator().run(measures,1,measures.getStackSize());
		IJ.run(imgDetect, "Gaussian Blur...", "sigma="+(1/(2*vX))+" stack");
		//	anna.remember("Parametre de lissage utilise, en pixel","sigma="+(1/(2*vX)));
		//	anna.storeImage(imgDetect, "Image de score lissee");
		imgDetect.getProcessor().resetMinAndMax();
		imgDetect.show();
		imgDetect.setTitle("Score map for inoculation detection");
		VitimageUtils.waitFor(5000);
		imgDetect.hide();
		double[][]coordMax=TransformUtils.getCoordinatesOf(imgDetect,VitimageUtils.COORD_OF_MAX_IN_TWO_LAST_SLICES,minPossibleZ*facteurAniso,maxPossibleZ*facteurAniso);
		System.out.println("Maximum relatif obtenu à ("+coordMax[0][0]+" , "+coordMax[0][1]+" ) soit, en coordonnees images : ( "+
				tabCoord[(int)Math.round(coordMax[0][0])][0]+" , "+tabCoord[(int)Math.round(coordMax[0][0])][1]+" , "+
				((coordMax[0][1]-facteurAniso/2.0)/facteurAniso)+" )");
		System.out.println("Maximum absolu obtenu à ("+coordMax[1][0]+" , "+coordMax[1][1]+" ) soit, en coordonnees images : ( "+
				tabCoord[(int)Math.round(coordMax[1][0])][0]+" , "+tabCoord[(int)Math.round(coordMax[1][0])][1]+" , "+
				((coordMax[1][1]-facteurAniso/2.0)/facteurAniso)+" )");
		Point3d inocPoint=TransformUtils.convertPointToRealSpace(new Point3d( tabCoord[(int)Math.round(coordMax[1][0])][0],  tabCoord[(int)Math.round(coordMax[1][0])][1],  ((coordMax[1][1]-facteurAniso/2.0)/facteurAniso) ),img) ;

		inocPoint=VitiDialogs.inspectInoculationPoint( imgCheck ,inocPoint);

		
		
		Point3d origine=TransformUtils.convertPointToRealSpace(new Point3d( xMax/2.0 , yMax/2.0 ,0),img);
		origine.z=inocPoint.z;

		double[]vect=new double[] {inocPoint.x - origine.x , inocPoint.y - origine.y , inocPoint.z - origine.z };
		double[]vectNorm=TransformUtils.normalize(vect);
		System.out.println(TransformUtils.stringVector(vectNorm,"Vecteur normalisé de la moelle vers le point d inoculation"));
		Point3d originePlusDinoc=new Point3d( origine.x + vectNorm[0] , origine.y + vectNorm[1] , inocPoint.z  + vectNorm[2]);
		Point3d originePlusDz=new Point3d( origine.x  ,origine.y , origine.z + 1 );
		Point3d[]ret=new Point3d[] { origine ,  originePlusDz , originePlusDinoc , inocPoint};
		imgCheck.close();
		return ret;		
	}
*/

	
	
	
	public static double[] caracterizeBackgroundOfImage(ImagePlus img) {
		int samplSize=Math.min(15,img.getWidth()/10);
		int x0=samplSize;
		int y0=samplSize;
		int x1=img.getWidth()-samplSize;
		int y1=img.getHeight()-samplSize;
		int z01=img.getStackSize()/2;
		double[][] vals=new double[4][2];
		vals[0]=VitimageUtils.valuesOfImageAround(img,x0,y0,z01,samplSize/2);
		vals[1]=VitimageUtils.valuesOfImageAround(img,x0,y1,z01,samplSize/2);
		vals[2]=VitimageUtils.valuesOfImageAround(img,x1,y0,z01,samplSize/2);
		vals[3]=VitimageUtils.valuesOfImageAround(img,x1,y1,z01,samplSize/2);		
		//System.out.println("");
		double [][]stats=new double[4][2];
		double []globStats=VitimageUtils.statistics2D(vals);
		//System.out.println("Background statistics averaged on the four corners = ( "+globStats[0]+" , "+globStats[1]+" ) ");
		for(int i=0;i<4;i++) {
			stats[i]=(VitimageUtils.statistics1D(vals[i]));
			//System.out.println("  --> Stats zone "+i+" =  ( "+stats[i][0]+" , "+stats[i][1]+")");
			if( (Math.abs(stats[i][0]-globStats[0])/globStats[0]>0.3)){
				System.out.println("Warning : noise computation  There should be an object in the supposed background\nthat can lead to misestimate background values. Detected at slice "+samplSize/2+"at "+
							(i==0 ?"Up-left corner" : i==1 ? "Down-left corner" : i==2 ? "Up-right corner" : "Down-right corner")+
							". Mean values of squares="+globStats[0]+". Outlier value="+vals[i][0]+" you should inspect the image and run again.");
				//img.show();
			}
		}
		return new double[] {globStats[0],globStats[1]};
	}
	

	
	
	/*
	public static ImagePlus areaOfPertinentComputation2 (ImagePlus img2,double sigmaGaussMapInPixels){
		int zMax=img2.getStackSize();
		double vX=img2.getCalibration().pixelWidth;
		double []val=Acquisition.caracterizeBackgroundOfImage(img2);
		double mu=val[0];
		double sigma=val[1];
		double thresh=mu+3*sigma;
		ImagePlus img=VitimageUtils.gaussianFiltering(img2,vX*sigmaGaussMapInPixels,vX*sigmaGaussMapInPixels,vX*sigmaGaussMapInPixels);
		System.out.println("Mu fond="+mu+" , Sigma fond="+sigma+" , Thresh="+thresh );
		img.getStack().getProcessor(1).set(0);
		if(zMax>3) {
			img.getStack().getProcessor(2).set(0);
			img.getStack().getProcessor(zMax).set(0);
			img.getStack().getProcessor(zMax-1).set(0);
		}
		ImagePlus imgMask0=VitimageUtils.getBinaryMask(img,thresh);
		ImagePlus imgMask1=VitimageUtils.connexe(imgMask0,1,256, 0,10E10, 6, 1,false);//L objet
		ImagePlus imgMask2=VitimageUtils.connexe(imgMask0,1,256, 0,10E10, 6, 2,false);//le cap
		IJ.run(imgMask1,"8-bit","");
		IJ.run(imgMask2,"8-bit","");
		ImageCalculator ic=new ImageCalculator();
		ImagePlus imgMask3=ic.run("OR create stack", imgMask1,imgMask2);							
		return imgMask3;
	}
*/

	/*
	public static ImagePlus areaOfPertinentMRIMapComputation (ImagePlus img2,double sigmaGaussMapInPixels){
		double voxVolume=VitimageUtils.getVoxelVolume(img2);
		int nbThreshObjects=100;
		double vX=img2.getCalibration().pixelWidth;
		double []val=Acquisition.caracterizeBackgroundOfImage(img2);
		double mu=val[0];
		double sigma=val[1];
		double thresh=mu+3*sigma;
		ImagePlus img=VitimageUtils.gaussianFiltering(img2,vX*sigmaGaussMapInPixels,vX*sigmaGaussMapInPixels,vX*sigmaGaussMapInPixels);
		System.out.println("Mu fond="+mu+" , Sigma fond="+sigma+" , Thresh="+thresh );
		ImagePlus imgMask0=VitimageUtils.getBinaryMask(img,thresh);
		ImagePlus imgMask1=VitimageUtils.connexe(imgMask0,1,256, nbThreshObjects*voxVolume,10E10, 26, 0,false);//L objet
		System.out.println(nbThreshObjects);
		ImagePlus imgMask2=VitimageUtils.getBinaryMask(imgMask1,1);
		return imgMask2;
	}
	*/
	

	public static ImagePlus restrictionMaskForFadingHandling (ImagePlus img,int marginOut){
		int dimX=img.getWidth(); int dimY=img.getHeight(); int dimZ=img.getStackSize();
		if(marginOut>dimZ/3)marginOut=dimZ/3;
		ImagePlus ret=IJ.createImage("MaskRegistration_"+img.getTitle(), dimX, dimY, dimZ, 8);
		VitimageUtils.adjustImageCalibration(ret,img);
		for(int z=0;z<dimZ;z++) {
			if((z<marginOut) || ( (dimZ-z)<marginOut))continue;
			byte []tabRet=(byte[])ret.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<dimX;x++) {
				for(int y=0;y<dimY;y++) {
					tabRet[dimX*y+x]=(byte)(255 & 0xff);
				}
			}
		}
		return ret;
	}
	
	
	
	public static ImagePlus convertFloatToShortWithoutDynamicChanges(ImagePlus imgIn) {
		ImagePlus ret=new Duplicator().run(imgIn);
		IJ.run(ret,"16-bit","");
		float[][] in=new float[imgIn.getStackSize()][];
		short[][] out=new short[ret.getStackSize()][];
		int index;
		int X=imgIn.getWidth();
		for(int z=0;z<imgIn.getStackSize();z++) {
			in[z]=(float []) imgIn.getStack().getProcessor(z+1).getPixels();
			out[z]=(short []) ret.getStack().getProcessor(z+1).getPixels();

			for(int x=0;x<imgIn.getWidth();x++) {
				for(int y=0;y<imgIn.getHeight();y++) {
					out[z][y*X+x]=(short)((int)(Math.round(in[z][y*X+x])));
				}			
			}
		}
		return ret;
	}
	public static int[] getMinMaxByte(ImagePlus imgIn) {
		byte[]in=new byte[imgIn.getStackSize()];
		int X=imgIn.getWidth();
		int Y=imgIn.getHeight();
		int Z=imgIn.getStackSize();
		int valMin=255;
		int valMax=0;
		for(int z=0;z<Z;z++) {
			in=(byte []) imgIn.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<X;x++) {
				for(int y=0;y<Y;y++) {
					if(in[y*X+x]>((byte)(valMax & 0xff))) valMax=(int)((byte)in[y*X+x] & 0xff);
					if(in[y*X+x]<((byte)(valMin & 0xff))) valMin=(int)((byte)in[y*X+x] & 0xff);
				}			
			}
		}
		return new int[] {valMin,valMax};
	}
	
	
	public static ImagePlus edges3DByte(ImagePlus imgRef,double sigma) {
		ImagePlus img=VitimageUtils.imageCopy(imgRef);
		IJ.run(img,"8-bit","");
		double alpha=2.0/(sigma+1);
		CannyEdge3D edges=new CannyEdge3D(ImageHandler.wrap(img),alpha);
		ImageHandler[] gg = edges.getGradientsXYZ();
		ImageHandler ed = edges.getEdge();
		double max=(125/Math.pow(alpha ,3));
		ImagePlus out=ed.getImagePlus();
		out.setDisplayRange(0,max);
		IJ.run(out,"8-bit","");
		return out;
	}

	
	
	public static double[] getMinMaxFloat(ImagePlus imgIn) {
		float[]in=new float[imgIn.getStackSize()];
		int X=imgIn.getWidth();
		int Y=imgIn.getHeight();
		int Z=imgIn.getStackSize();
		float valMin=(float)10E10;
		float valMax=(float)(-10E10);
		for(int z=0;z<Z;z++) {
			in=(float []) imgIn.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<X;x++) {
				for(int y=0;y<Y;y++) {
					if(in[y*X+x]>valMax)valMax=in[y*X+x];
					if(in[y*X+x]<valMin)valMin=in[y*X+x];
				}			
			}
		}
		return new double[] {valMin,valMax};
	}

	public static ImagePlus convertShortToFloatWithoutDynamicChanges(ImagePlus imgIn) {
		ImagePlus ret=new Duplicator().run(imgIn);
		IJ.run(ret,"32-bit","");
		float[][] out=new float[ret.getStackSize()][];
		short[][] in=new short[imgIn.getStackSize()][];
		int index;
		int X=imgIn.getWidth();
		for(int z=0;z<imgIn.getStackSize();z++) {
			out[z]=(float []) ret.getStack().getProcessor(z+1).getPixels();
			in[z]=(short []) imgIn.getStack().getProcessor(z+1).getPixels();

			for(int x=0;x<imgIn.getWidth();x++) {
				for(int y=0;y<imgIn.getHeight();y++) {
					out[z][y*X+x]=((int)((in[z][y*X+x] & 0xffff )));
				}			
			}
		}
		return ret;
	}
	
	
	
	
	public static void adjustImageOnScreen(ImagePlus img,int xPosition,int yPosition) {
		adjustFrameOnScreen(img.getWindow(), xPosition, yPosition);
	}

	public static void adjustFrameOnScreen(Frame frame,int xPosition,int yPosition) {
		int border=50;//taskbar, or things like this
        java.awt.Dimension currentScreen = Toolkit.getDefaultToolkit().getScreenSize();
        int screenX=(int)Math.round(currentScreen.width);
        int screenY=(int)Math.round(currentScreen.height);
        if(screenX>1920)screenX/=2;        
        java.awt.Dimension currentDims=frame.getSize();
        int frameX=(int)Math.round(currentDims.width);
        int frameY=(int)Math.round(currentDims.height);

        int x=0;int y=0;
        if(xPosition==0)x=border;
        if(xPosition==1)x=(screenX-frameX)/2;
        if(xPosition==2)x=screenX-border-frameX;
        if(yPosition==0)y=border;
        if(yPosition==1)y=(screenY-frameY)/2;
        if(yPosition==2)y=screenY-border-frameY;
		int xx=frame.getSize().width;
		int yy=frame.getSize().height;		
		frame.setLocation(x, y);
		frame.setSize(xx, yy);
        //System.out.println("Positioning frame at screen coordinates : ( "+x+" , "+y+" )");
    }

	
	public static void adjustImageOnScreenRelative(ImagePlus img1,ImagePlus img2Reference,int xPosition,int yPosition,int distance) {
		adjustFrameOnScreenRelative(img1.getWindow(),img2Reference.getWindow(), xPosition, yPosition,distance);
	}

	public static void adjustFrameOnScreenRelative(Frame currentFrame,Frame referenceFrame,int xPosition,int yPosition,int distance) {
		if(xPosition==3) {currentFrame.setLocation(referenceFrame.getLocationOnScreen().x,referenceFrame.getLocationOnScreen().y);return;}
		java.awt.Dimension currentScreen = Toolkit.getDefaultToolkit().getScreenSize();
        int screenX=(int)Math.round(currentScreen.width);
        int screenY=(int)Math.round(currentScreen.height);
        if(screenX>1920)screenX/=2;        

        java.awt.Dimension currentDims=currentFrame.getSize();
        int currentX=(int)Math.round(currentDims.width);
        int referenceX=(int)Math.round(referenceFrame.getSize().width);
        int currentY=(int)Math.round(currentDims.height);
		int border=50;//taskbar, or things like this
		int x=0;int y=0;
        if(xPosition==0)x=referenceFrame.getLocationOnScreen().x-currentX-distance;//Set to the left of reference
        if(xPosition==2)x=referenceFrame.getLocationOnScreen().x+referenceX+distance;
        if(yPosition==0)y=border;
        if(yPosition==1)y=(screenY-currentY)/2;
        if(yPosition==2)y=screenY-border-currentY;
        currentFrame.setLocation(x, y);
        //System.out.println("Positioning frame at screen coordinates : ( "+x+" , "+y+" )");
    }


	
	public static double[][]getDoubleArray2DCopy(double[][]in){
		double[][]ret=new double[in.length][];
		for(int i=0;i<in.length;i++) {
			ret[i]=new double[in[i].length];
			for(int j=0;j<in[i].length;j++) {
				ret[i][j]=in[i][j];
			}
		}
		return ret;
	}
	
	public static Point3d[][] getRegistrationLandmarks(int minimumNbWantedPointsPerImage,ImagePlus imgRef,ImagePlus imgMov){
		ImagePlus imgRefBis=imgRef.duplicate();		imgRefBis.getProcessor().resetMinAndMax();		imgRefBis.show();		imgRefBis.setTitle("Reference image");
		ImagePlus imgMovBis=imgMov.duplicate();	imgMovBis.getProcessor().resetMinAndMax();		imgMovBis.show();		imgMovBis.setTitle("Moving image");
		Point3d []pRef=new Point3d[1000];
		Point3d []pMov=new Point3d[1000];
		RoiManager rm=RoiManager.getRoiManager();
		rm.reset();
		IJ.setTool("point");
		IJ.showMessage("Examine images and click on reference points");
		boolean finished =false;
		do {
			if(rm.getCount()>=minimumNbWantedPointsPerImage*2 ) finished=true;
			try {
				java.util.concurrent.TimeUnit.MILLISECONDS.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}while (!finished);
		int nbCouples=0;
		for(int indP=0;indP<rm.getCount()/2;indP++){
			pRef[indP]=new Point3d(rm.getRoi(indP*2 ).getXBase() , rm.getRoi(indP * 2).getYBase() ,  rm.getRoi(indP * 2).getZPosition());
			pMov[indP]=new Point3d(rm.getRoi(indP*2 +1 ).getXBase() , rm.getRoi(indP * 2 +1 ).getYBase() ,  rm.getRoi(indP * 2 +1 ).getZPosition());
			pRef[indP]=TransformUtils.convertPointToRealSpace(pRef[indP],imgRef);
			pMov[indP]=TransformUtils.convertPointToRealSpace(pMov[indP],imgRef);
			nbCouples++;
		}
		System.out.println("Number of correspondance pairs = "+nbCouples);
		imgRefBis.close();
		imgMovBis.close();
		Point3d []pRefRet=new Point3d[nbCouples];		Point3d []pMovRet=new Point3d[nbCouples];
		for(int i=0;i<pRefRet.length;i++) {
			pRefRet[i]=pRef[i];
			pMovRet[i]=pMov[i];
		}
		return new Point3d[][] {pRefRet,pMovRet};
	}
	
	
	


	
	
	public static ImagePlus convertByteToFloatWithoutDynamicChanges(ImagePlus imgIn) {
		ImagePlus ret=VitimageUtils.imageCopy(imgIn);
		IJ.run(ret,"32-bit","");

		float[][] out=new float[ret.getStackSize()][];
		byte[][] in=new byte[imgIn.getStackSize()][];
		int index;
		int X=imgIn.getWidth();
		for(int z=0;z<imgIn.getStackSize();z++) {
			out[z]=(float []) ret.getStack().getProcessor(z+1).getPixels();
			in[z]=(byte []) imgIn.getStack().getProcessor(z+1).getPixels();

			for(int x=0;x<imgIn.getWidth();x++) {
				for(int y=0;y<imgIn.getHeight();y++) {
					out[z][y*X+x]=((int)((in[z][y*X+x] & 0xff )));
				}			
			}
		}
		return ret;
	}
	

	public static double getOtsuThreshold(ImagePlus img) {
		OtsuThresholdImageFilter otsu=new OtsuThresholdImageFilter();
		otsu.execute(ItkImagePlusInterface.imagePlusToItkImage(img));
		return otsu.getThreshold();
	}

	public static ImagePlus otsuThreshold(ImagePlus img) {
		OtsuThresholdImageFilter otsu=new OtsuThresholdImageFilter();
		otsu.setInsideValue((short)0);
		otsu.setOutsideValue((short)255);
		return(ItkImagePlusInterface.itkImageToImagePlus(otsu.execute(ItkImagePlusInterface.imagePlusToItkImage(img))));
	}

	public static ImagePlus multiplyImage(ImagePlus imgIn,double val) {
		ImagePlus img=new Duplicator().run(imgIn);
		IJ.run(img,"Multiply...","value="+val+" stack");
		return img;
	}
	
	
	public static ImagePlus[]computeGradients(ImagePlus img,double radius,boolean naive,boolean smooth){
		boolean debug=true;
		ImagePlus []res=null;
		if(naive)res= VitimageUtils.yoloGradients(img, (int)Math.round(radius),0.00001,100000,false);
		else {
			GradientImageFilter gif=new GradientImageFilter();
			res=ItkImagePlusInterface.convertDisplacementFieldFloatToImagePlusArray(gif.execute(ItkImagePlusInterface.imagePlusToItkImage(img),false,false));	
		}
		for(int d=0;d<3;d++)res[d]=VitimageUtils.gaussianFiltering(res[d],radius, radius, radius);
		if(debug)res[0].show();
		res[0].setTitle("x");
		res[0].setDisplayRange(-2, 2);
		if(debug)res[1].show();
		res[1].setTitle("y");
		res[1].setDisplayRange(-2, 2);
		if(debug)res[2].show();
		res[2].setTitle("z");
		res[2].setDisplayRange(-2, 2);

		return res;
	}
	

	
	
	
	public static ImagePlus imageCopy(ImagePlus imgRef) {
		ImagePlus ret=new Duplicator().run(imgRef);
		VitimageUtils.adjustImageCalibration(ret, imgRef);
		return ret;
	}
	
	public static ImagePlus imageCopy(ImagePlus imgRef,String title) {
		ImagePlus ret=new Duplicator().run(imgRef);
		VitimageUtils.adjustImageCalibration(ret, imgRef);
		for(int z=0;z<imgRef.getStackSize();z++) {
			ret.getStack().setSliceLabel(title+"_z="+(z+1), z+1);
		}
		ret.setTitle(title);
		return ret;
	}

	
	
	public static ImagePlus[]decomposeRGBImage(ImagePlus imgRGB){
		return ChannelSplitter.split(imgRGB);
	}
	
	public static ImagePlus compositeRGBByte(ImagePlus img1Source,ImagePlus img2Source,ImagePlus img3Source,double coefR,double coefG,double coefB){
		ImagePlus img1=new Duplicator().run(img1Source);
		ImagePlus img2=new Duplicator().run(img2Source);
		ImagePlus img3=new Duplicator().run(img3Source);
		IJ.run(img1,"Multiply...","value="+coefR+" stack");
		IJ.run(img2,"Multiply...","value="+coefG+" stack");
		IJ.run(img3,"Multiply...","value="+coefB+" stack");
		ImageStack is=RGBStackMerge.mergeStacks(img1.getStack(),img2.getStack(),img3.getStack(),true);
		ImagePlus img=new ImagePlus("Composite",is);
		VitimageUtils.adjustImageCalibration(img, img1Source);
		return img;
	}

	public static ImagePlus balanceColors(ImagePlus imgIn,int radius) {
		System.out.println("\n Normalisation image, avec rayon="+radius);
		ImagePlus img=new Duplicator().run(imgIn);
		ImagePlus[] channels = ChannelSplitter.split(img);
		int xM=channels[0].getWidth();		
		int yM=channels[1].getHeight();
		int zM=channels[2].getStackSize();
		ImagePlus[][] tabImg=new ImagePlus[3][zM];
		double[]tempVals;
		double [][][] vals=new double[3][zM][5];
		for(int can=0;can<3;can++) {
			for(int z=0;z<zM;z++) {
				channels[can].setSlice(z+1);
				tabImg[can][z]=channels[can].crop();
				tempVals=VitimageUtils.valuesOfBlock(tabImg[can][z],0, 0, 0, radius, radius, 0);vals[can][z][0]=VitimageUtils.statistics1D(tempVals)[0];
				tempVals=VitimageUtils.valuesOfBlock(tabImg[can][z],xM-radius-1, 0, 0, xM-1, radius, 0);vals[can][z][1]=VitimageUtils.statistics1D(tempVals)[0];
				tempVals=VitimageUtils.valuesOfBlock(tabImg[can][z],0, yM-radius-1, 0, radius, yM-1, 0);vals[can][z][2]=VitimageUtils.statistics1D(tempVals)[0];
				tempVals=VitimageUtils.valuesOfBlock(tabImg[can][z],xM-radius-1,  yM-radius-1, 0, xM-1, yM-1, 0);vals[can][z][3]=VitimageUtils.statistics1D(tempVals)[0];
				vals[can][z][4]=0.25*(vals[can][z][0]+vals[can][z][1]+vals[can][z][2]+vals[can][z][3]);
				//System.out.println("Canal="+can+" Z="+z+" Valeurs="+TransformUtils.stringVectorN(vals[can][z], "")+"");
				IJ.run(tabImg[can][z],"32-bit","");
				tabImg[can][z].getProcessor().setMinAndMax(0,vals[can][z][4]);
				IJ.run(tabImg[can][z],"8-bit","");
			}
			channels[can]=Concatenator.run(tabImg[can]);
		}
		ImageStack is=RGBStackMerge.mergeStacks(channels[0].getStack(),channels[1].getStack(),channels[2].getStack(),true);
		//ImageStack is=RGBStackMerge.mergeChannels(new ImagePlus[] {channels[0],channels[1],channels[2]},true).getStack();
		img=new ImagePlus("Composite",is);
		VitimageUtils.adjustImageCalibration(img, imgIn);
		return img;
	}

	
	public static ImagePlus compositeRGJetDouble(ImagePlus img1Source,ImagePlus img2Source,ImagePlus img3Source,double coefR,double coefJet,double coefB,String title){
		ImagePlus img1=new Duplicator().run(img1Source);
		ImagePlus img2=new Duplicator().run(img2Source);
		ImagePlus img3=new Duplicator().run(img3Source);
		img1.getProcessor().resetMinAndMax();
		img2.getProcessor().resetMinAndMax();
		img3.getProcessor().resetMinAndMax();
		IJ.run(img1,"8-bit","");
		IJ.run(img2,"8-bit","");
		IJ.run(img3,"8-bit","");
		IJ.run(img3,"Fire","");
		ImageStack is=RGBStackMerge.mergeStacks(img1.getStack(),img2.getStack(),img3.getStack(),true);
		ImagePlus img=new ImagePlus(title,is);
		VitimageUtils.adjustImageCalibration(img, img1Source);
		return img;
	}

	public static ImagePlus actualizeData(ImagePlus source,ImagePlus dest) {
		int[]dims=VitimageUtils.getDimensions(source);
		int Z=dims[2];int Y=dims[1];int X=dims[0];
		if(source.getType() == ImagePlus.GRAY8) {
			byte[][] valsSource=new byte[Z][];
			byte[][] valsDest=new byte[Z][];
			for(int z=0;z<Z;z++) {
				valsSource[z]=(byte [])source.getStack().getProcessor(z+1).getPixels();
				valsDest[z]=(byte [])dest.getStack().getProcessor(z+1).getPixels();
				for(int x=0;x<X;x++)for(int y=0;y<Y;y++)valsDest[z][y*X+x]=valsSource[z][y*X+x];
			}			
		}
		if(source.getType() == ImagePlus.GRAY16) {
			short[][] valsSource=new short[Z][];
			short[][] valsDest=new short[Z][];
			for(int z=0;z<Z;z++) {
				valsSource[z]=(short [])source.getStack().getProcessor(z+1).getPixels();
				valsDest[z]=(short [])dest.getStack().getProcessor(z+1).getPixels();
				for(int x=0;x<X;x++)for(int y=0;y<Y;y++)valsDest[z][y*X+x]=valsSource[z][y*X+x];
			}			
		}
		if(source.getType() == ImagePlus.GRAY32) {
			float[][] valsSource=new float[Z][];
			float[][] valsDest=new float[Z][];
			for(int z=0;z<Z;z++) {
				valsSource[z]=(float [])source.getStack().getProcessor(z+1).getPixels();
				valsDest[z]=(float [])dest.getStack().getProcessor(z+1).getPixels();
				for(int x=0;x<X;x++)for(int y=0;y<Y;y++)valsDest[z][y*X+x]=valsSource[z][y*X+x];
			}			
		}

		if(source.getType() == ImagePlus.COLOR_RGB) {
			int[][] valsSource=new int[Z][];
			int[][] valsDest=new int[Z][];
			for(int z=0;z<Z;z++) {
				valsSource[z]=(int [])source.getStack().getProcessor(z+1).getPixels();
				valsDest[z]=(int [])dest.getStack().getProcessor(z+1).getPixels();
				for(int x=0;x<X;x++)for(int y=0;y<Y;y++)valsDest[z][y*X+x]=valsSource[z][y*X+x];
			}			
		}
		//dest.changes=true;
		dest.updateAndDraw();
		return dest;
		
	}
	
	public static ImagePlus compositeRGBDoubleJet(ImagePlus img1,ImagePlus img2,ImagePlus img3,String title,boolean mask,int teinte) {
		ImagePlus img1Source=new Duplicator().run(img1);
		ImagePlus img2Source=new Duplicator().run(img2);
		ImagePlus img3Source=new Duplicator().run(img3);
		img1Source.resetDisplayRange();
		img2Source.resetDisplayRange();
		img3Source.resetDisplayRange();
		IJ.run(img1Source,"8-bit","");
		IJ.run(img2Source,"8-bit","");
		IJ.run(img3Source,"8-bit","");
		if(mask) {
			ImagePlus maskJet=VitimageUtils.thresholdByteImage(img3Source, 1, 256);
			IJ.run(maskJet,"Invert","");
			IJ.run(maskJet, "Divide...", "value=255");
			img1Source = new ImageCalculator().run("Multiply create", img1Source, maskJet);
			img2Source = new ImageCalculator().run("Multiply create", img2Source, maskJet);
		}
		IJ.run(img1Source,"Red","");
		IJ.run(img2Source,"Green","");
		if(teinte==0)IJ.run(img3Source,"Blue","");
		if(teinte==1)IJ.run(img3Source,"Grays","");
		if(teinte==2)IJ.run(img3Source,"Fire","");
		ImagePlus ret=RGBStackMerge.mergeChannels(new ImagePlus[] {img1Source,img2Source,img3Source},false);
		IJ.run(ret,"RGB Color","");
		return ret;
	}
	
	
	
	public static ImagePlus compositeRGBDouble(ImagePlus img1Source,ImagePlus img2Source,ImagePlus img3Source,double coefR,double coefG,double coefB,String title){
		ImagePlus img1=new Duplicator().run(img1Source);
		ImagePlus img2=new Duplicator().run(img2Source);
		ImagePlus img3=new Duplicator().run(img3Source);
		img1.getProcessor().resetMinAndMax();
		img2.getProcessor().resetMinAndMax();
		img3.getProcessor().resetMinAndMax();
		IJ.run(img1,"8-bit","");
		IJ.run(img2,"8-bit","");
		IJ.run(img3,"8-bit","");
//		img3.getProcessor().setMinAndMax(60, 200);
//		IJ.run(img3,"8-bit","");
//		IJ.run(img1,"Multiply...","value="+coefR+" stack");
		//		IJ.run(img2,"Multiply...","value="+coefG+" stack");
		//IJ.run(img3,"Multiply...","value="+coefB+" stack");
		ImageStack is=RGBStackMerge.mergeStacks(img1.getStack(),img2.getStack(),img3.getStack(),true);
		ImagePlus img=new ImagePlus(title,is);
		VitimageUtils.adjustImageCalibration(img, img1Source);
		return img;
	}
	
	
	public static ImagePlus compositeRGBLByte(ImagePlus img1Source,ImagePlus img2Source,ImagePlus img3Source,ImagePlus img4Source,double coefR,double coefG,double coefB,double coefL){
		ImagePlus img1=new Duplicator().run(img1Source);
		ImagePlus img2=new Duplicator().run(img2Source);
		ImagePlus img3=new Duplicator().run(img3Source);
		ImagePlus img4=new Duplicator().run(img4Source);
		IJ.run(img1,"Multiply...","value="+coefR+" stack");
		IJ.run(img2,"Multiply...","value="+coefG+" stack");
		IJ.run(img3,"Multiply...","value="+coefB+" stack");
		IJ.run(img4,"Multiply...","value="+coefL+" stack");
		ImagePlus img=RGBStackMerge.mergeChannels(new ImagePlus[] {img1,img2,img3,img4},true);
		VitimageUtils.adjustImageCalibration(img, img1Source);
		return img;
	}
	
	
	public static void showImageUntilItIsClosed(ImagePlus img,String texte) {
		img.setTitle(texte);
		img.show();
		img.setSlice(img.getStackSize()/2);
		while(WindowManager.getImage(texte)!=null) {
			VitimageUtils.waitFor(1000);
		}
		return ;
	}

	
	
	public static void showImageIn3D(ImagePlus img,boolean orthoSlice) {
    	ij3d.Image3DUniverse univ=new ij3d.Image3DUniverse();
		univ.show();
		if(orthoSlice) {
			univ.addOrthoslice(img, new Color3f(Color.white),"ref",50,new boolean[] {true,true,true},1);
		}
		else univ.addContent(img, new Color3f(Color.red),"ref",50,new boolean[] {true,true,true},1,0 );
		ij3d.ImageJ3DViewer.select("ref");
		univ.getSelected().showCoordinateSystem(true);
	}
		
    public static ItkTransform manualRegistrationIn3D(ImagePlus imRef,ImagePlus imMov) {
    	ImagePlus imgRef=new Duplicator().run(imRef);
    	VitimageUtils.adjustImageCalibration(imgRef,imRef);
    	imgRef.setTitle("imgRef3D");
    	ImagePlus imgMov=new Duplicator().run(imMov);
    	VitimageUtils.adjustImageCalibration(imgMov,imMov);
    	ij3d.Image3DUniverse univ=new ij3d.Image3DUniverse();
		univ.show();
		univ.addContent(imgRef, new Color3f(Color.red),"imgRef",50,new boolean[] {true,true,true},1,0 );
		univ.addContent(imgMov, new Color3f(Color.green),"imgMov",50,new boolean[] {true,true,true},1,0 );
		ij3d.ImageJ3DViewer.select("imgRef");
		ij3d.ImageJ3DViewer.lock();
		ij3d.ImageJ3DViewer.select("imgMov");
		int iter=0;    	
		imgRef.show();
		VitiDialogs.getYesNoUI("","Red volume is fixed, green volume can move.\n Use the 3d viewer to adjust the green volume with the red volume using the mouse.\n Click-drag=rotate , Shift+Click-drag=translate.\nClose imgRef3D to confirm transformation is done");		
		while(WindowManager.getImage("imgRef3D")!=null) {
			VitimageUtils.waitFor(1000);
			System.out.print(iter+++" ");
		}
    	
    	Transform3D tr=new Transform3D();
		double[]tab=new double[16];
		univ.getContent("imgMov").getLocalRotate().getTransform(tr);
		tr.get(tab);
		ItkTransform itRot=ItkTransform.array16ElementsToItkTransform(tab);
		univ.getContent("imgMov").getLocalTranslate().getTransform(tr);
		tr.get(tab);
		ItkTransform itTrans=ItkTransform.array16ElementsToItkTransform(tab);
		itTrans.addTransform(itRot);
		itTrans=itTrans.simplify();
		System.out.println("Global transform computed : "+itTrans);
		ItkTransform ret=new ItkTransform(itTrans.getInverse());
		univ.removeAllContents();
		univ.close();
    	univ=null;    
    	return ret;
    }
        
	
	public static ImagePlus compositeOf(ImagePlus img1Source,ImagePlus img2Source){
		ImagePlus img1=new Duplicator().run(img1Source);
		ImagePlus img2=new Duplicator().run(img2Source);
		img1.resetDisplayRange();
		img2.resetDisplayRange();
//		img1.getProcessor().resetMinAndMax();
//		img2.getProcessor().resetMinAndMax();
		IJ.run(img1,"8-bit","");
		IJ.run(img2,"8-bit","");
		ImageStack is=RGBStackMerge.mergeStacks(img1.getStack(),img2.getStack(),null,true);
		ImagePlus img=new ImagePlus("Composite",is);
		VitimageUtils.adjustImageCalibration(img, img1Source);
		return img;
	}


	public static Point3d[]convertDoubleArrayToPoint3dArray(double[][]tab){
		Point3d[]tabPt=new Point3d[tab.length];
		for(int i=0;i<tab.length;i++)tabPt[i]=new Point3d(tab[i][0],tab[i][1],tab[i][2]);
		return tabPt;		
	}

	public static double[][]convertPoint3dArrayToDoubleArray(Point3d[]tab){
		double[][]tabPt=new double[tab.length][3];
		for(int i=0;i<tab.length;i++){
			tabPt[i][0]=tab[i].x;
			tabPt[i][1]=tab[i].y;
			tabPt[i][2]=tab[i].z;
		}
		return tabPt;		
	}
	
	public static ImagePlus compositeNoAdjustOf(ImagePlus img1Source,ImagePlus img2Source){
		ImagePlus img1=new Duplicator().run(img1Source);
		ImagePlus img2=new Duplicator().run(img2Source);
		IJ.run(img1,"8-bit","");
		IJ.run(img2,"8-bit","");
		ImageStack is=RGBStackMerge.mergeStacks(img1.getStack(),img2.getStack(),null,true);
		ImagePlus img=new ImagePlus("Composite",is);
		VitimageUtils.adjustImageCalibration(img, img1Source);
		return img;
	}
	
	public static ImagePlus compositeOf(ImagePlus img1,ImagePlus img2,String title){
		ImagePlus composite=compositeOf(img1,img2);
		composite.setTitle(title);
		return composite;
	}

	public static ImagePlus compositeNoAdjustOf(ImagePlus img1,ImagePlus img2,String title){
		ImagePlus composite=compositeNoAdjustOf(img1,img2);
		composite.setTitle(title);
		return composite;
	}

	public static ImagePlus writeBlackTextOnImage(String text, ImagePlus img,int fontSize,int numLine) {
		ImagePlus ret=new Duplicator().run(img);
		Font font = new Font("SansSerif", Font.PLAIN, fontSize);
		TextRoi roi = new TextRoi(10*img.getWidth()*1.0/512,10*img.getWidth()*1.0/512+numLine*fontSize*2, text, font);
		roi.setStrokeColor(Color.black);
		Overlay overlay = new Overlay();
		overlay.add(roi);
		ret.setOverlay(overlay); 
		Roi[] ovlArray = ret.getOverlay().toArray();
		for (Roi ro: ovlArray) {
			ret.setRoi(ro);
			IJ.run(ret, "Draw", "stack");
			ret.setRoi((Roi)null);
		}
		return ret;
	}

	public static ImagePlus writeTextOnImage(String text, ImagePlus img,int fontSize,int numLine) {
		ImagePlus ret=new Duplicator().run(img);
		Font font = new Font("SansSerif", Font.PLAIN, fontSize);
		TextRoi roi = new TextRoi(10*img.getWidth()*1.0/512,10*img.getWidth()*1.0/512+numLine*fontSize*2, text, font);
		roi.setStrokeColor(Color.white);
		Overlay overlay = new Overlay();
		overlay.add(roi);
		ret.setOverlay(overlay); 
		Roi[] ovlArray = ret.getOverlay().toArray();
		for (Roi ro: ovlArray) {
			ret.setRoi(ro);
			IJ.run(ret, "Draw", "stack");
			ret.setRoi((Roi)null);
		}
		return ret;
	}

	public static ImagePlus writeTextOnImage(String text, ImagePlus img,int fontSize,int numLine,double value) {
		return writeTextOnImage(text,  img, fontSize,numLine,value,10.0/512, 10.0/512);
	}

	public static ImagePlus writeWhiteOnImage(String text, ImagePlus img,int fontSize,int numLine) {
		return writeTextOnImage(text,  img, fontSize,numLine,EPSILON,10.0/512, 10.0/512);
	}
	public static ImagePlus writeTextOnImage(String text, ImagePlus img,int fontSize,int numLine,double value,double xCoordRatio,double yCoordRatio) {
		double valMin=img.getProcessor().getMin();
		double valMax=img.getProcessor().getMax();
		ImagePlus ret=new Duplicator().run(img);
		if(value!=EPSILON)ret.getProcessor().setMinAndMax(valMin, value);
		Font font = new Font("SansSerif", Font.PLAIN, fontSize);
		TextRoi roi = new TextRoi( xCoordRatio*img.getWidth(),yCoordRatio*img.getWidth()+numLine*fontSize*2, text, font);
		roi.setStrokeColor(Color.white);
		Overlay overlay = new Overlay();
		overlay.add(roi);
		ret.setOverlay(overlay); 
		Roi[] ovlArray = ret.getOverlay().toArray();
		for (Roi ro: ovlArray) {
			ret.setRoi(ro);
			IJ.run(ret, "Draw", "stack");
			ret.setRoi((Roi)null);
		}
		if(value!=EPSILON)ret.getProcessor().setMinAndMax(valMin, valMax);
		return ret;
	}


	public static ImagePlus removeEveryTitleTextInStandardFloatImage(ImagePlus img) {
		int heightMax=50;
		ImagePlus ret=new Duplicator().run(img);
		
		if(img.getType() != ImagePlus.GRAY32)return null;
		int xM=img.getWidth();
		int yM=img.getHeight();
		int zM=img.getStackSize();
		float[]valsImg;
		for(int z=0;z<zM;z++) {
			valsImg=(float [])ret.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<xM;x++) {
				for(int y=0;y<yM;y++) {
					if (y<heightMax) {
						valsImg[xM*y+x]=0; 
					}
				}
			}			
		}
		return ret;
	}

	public static void putThatImageInThatOther(ImagePlus source,ImagePlus dest) {
		int dimX= source.getWidth(); int dimY= source.getHeight(); int dimZ= source.getStackSize();
		for(int z=0;z<dimZ;z++) {
			int []tabDest=(int[])dest.getStack().getProcessor(z+1).getPixels();
			int []tabSource=(int[])source.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<dimX;x++) {
				for(int y=0;y<dimY;y++) {
					tabDest[dimX*y+x]=tabSource[dimX*y+x];
				}
			}
		}
	}


	public static void adjustImageCalibration(ImagePlus img,double []voxSize,String unit) {
		if(img==null)return;
		img.getCalibration().setUnit(unit);
		Calibration cal = img.getCalibration();			
		cal.pixelWidth =voxSize[0];
		cal.pixelHeight =voxSize[1];
		cal.pixelDepth =voxSize[2];
	}

	public static void adjustImageCalibration(ImagePlus img,ImagePlus ref) {
		if(img==null)return;
		img.getCalibration().setUnit(ref.getCalibration().getUnit());
		img.getCalibration().pixelWidth=ref.getCalibration().pixelWidth;
		img.getCalibration().pixelHeight=ref.getCalibration().pixelHeight;
		img.getCalibration().pixelDepth=ref.getCalibration().pixelDepth;
	}

	public static void soundAlert(String message) {
		if(message.equals("Inoculation")) {
			try {
				Runtime.getRuntime().exec("aplay /home/fernandr/Audio/Inoc.wav &");
				Runtime.getRuntime().exec("aplay /home/fernandr/Audio/Inoc2.wav &");
				VitimageUtils.waitFor(5000);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else if(message.equals("Beep")) {
			try {
				Runtime.getRuntime().exec("aplay /home/fernandr/Audio/Beep.wav &");
			
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else {
			IJ.showMessage("Message not understood. Cannot say it");
		}
	}
	
	
	//TODO : order / sort functions with a logical meaning in sorting. Maybe constitute classes to that end.
	
	
	public static String strInt6chars(int nb) {
		if(nb<100000 && nb>9999)  return new String(" "+nb);
		if(nb<10000 && nb>999)    return new String("  "+nb);
		if(nb<1000 && nb>99)      return new String("   "+nb);
		if(nb<100 && nb>9)        return new String("    "+nb);
		if(nb<10)                 return new String("     "+nb);
		                          return new String(""+nb);
	}
		
	public static double getVoxelVolume(ImagePlus img) {
		return img.getCalibration().pixelDepth*img.getCalibration().pixelWidth*img.getCalibration().pixelHeight;
	}

	public static ImagePlus drawCircleInImage(ImagePlus imgIn,double ray,int x0,int y0,int z0) {
		if(imgIn.getType() != ImagePlus.GRAY8)return imgIn;
		ImagePlus img=new Duplicator().run(imgIn);
		int xM=img.getWidth();
		int yM=img.getHeight();
		int zM=img.getStackSize();
		double voxSX=img.getCalibration().pixelWidth;
		double voxSY=img.getCalibration().pixelHeight;
		double voxSZ=img.getCalibration().pixelDepth;
		double realDisX;
		double realDisY;
		double realDisZ;
		byte[][] valsImg=new byte[zM][];
		double distance;
		for(int z=0;z<zM;z++) {
			valsImg[z]=(byte [])img.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<xM;x++) {
				for(int y=0;y<yM;y++) {
					realDisX=(x-x0)*voxSX;
					realDisY=(y-y0)*voxSY;
					realDisZ=(z-z0)*voxSZ;
					distance=Math.sqrt( realDisX * realDisX  +  realDisY * realDisY  + realDisZ * realDisZ  );
					if(distance < ray) {
						valsImg[z][xM*y+x]=  (byte)( 122 & 0xff);
					}
				}
			}			
		}
		return img;
	}

	public static int indmax(int[]tab) {
		double max=-1000000;int indmax=0;
		for(int i=0;i<tab.length;i++)if(tab[i]>max) {max=tab[i];indmax=i;}
		return indmax;
	}

	public static int indmax(double[]tab) {
		double max=-1000000;int indmax=0;
		for(int i=0;i<tab.length;i++)if(tab[i]>max) {max=tab[i];indmax=i;}
		return indmax;
	}
	
	//http://www.tomgibara.com/computer-vision/CannyEdgeDetector.java
	public static ImagePlus cannyDericheGradient(ImagePlus sourceImageBis,float gaussianKernelRadiusBis ) {
		float gaussianKernelRadius=gaussianKernelRadiusBis;
		int gaussianKernelWidth=(int)Math.round(gaussianKernelRadius);
		if(gaussianKernelWidth<10 && gaussianKernelWidth>3)gaussianKernelWidth-=1;
		if(gaussianKernelRadius==1)gaussianKernelWidth++;
		if (gaussianKernelRadius <1.1)gaussianKernelRadius=(float)1.3;
//		else if(gaussianKernelWidth<4 && gaussianKernelWidth>1)gaussianKernelWidth++;
		ImagePlus sourceImage=VitimageUtils.imageCopy(sourceImageBis);
		int width = sourceImage.getWidth();
		int height = sourceImage.getHeight();
		int picsize = width * height;
		int[]data = new int[picsize];
		int[]magnitude = new int[picsize];

		float[]xConv = new float[picsize];
		float[]yConv = new float[picsize];
		float[]xGradient = new float[picsize];
		float[]yGradient = new float[picsize];
		ImageProcessor ip = sourceImage.getProcessor();
		ip = ip.convertToByte(true);
		for (int i=0; i<ip.getPixelCount(); i++)data[i] = ip.get(i);
		computeGradients(gaussianKernelRadius, gaussianKernelWidth,xConv,yConv,xGradient,yGradient,data,magnitude,width,height,false);
		ImagePlus out=VitimageUtils.imageCopy(sourceImage);
		IJ.run(out,"16-bit","");
		ImageProcessor ip2 = out.getProcessor();
		for (int i=0; i<ip2.getPixelCount(); i++)ip2.set(i,magnitude[i]);
		out.setProcessor(ip2);
		out.setDisplayRange(0,255);		
		IJ.run(out,"8-bit","");
		return  out;
	}
	
	
	
	

	public static void computeGradients(float kernelRadius, int kernelWidth,float[]xConv,float[]yConv,float[]xGradient,float[]yGradient,int[]data,int[]magnitude,int width,int height,boolean keepOnlyMaxima) {
		
		//generate the gaussian convolution masks
		float kernel[] = new float[kernelWidth];
		float diffKernel[] = new float[kernelWidth];
		int kwidth;
		for (kwidth = 0; kwidth < kernelWidth; kwidth++) {
			float g1 = gaussian(kwidth, kernelRadius);
			if (g1 <= GAUSSIAN_CUT_OFF && kwidth >= 2) break;
			float g2 = gaussian(kwidth - 0.5f, kernelRadius);
			float g3 = gaussian(kwidth + 0.5f, kernelRadius);
			kernel[kwidth] = (g1 + g2 + g3) / 3f / (2f * (float) Math.PI * kernelRadius * kernelRadius);
			diffKernel[kwidth] = g3 - g2;
		}

		int initX = kwidth - 1;
		int maxX = width - (kwidth - 1);
		int initY = width * (kwidth - 1);
		int maxY = width * (height - (kwidth - 1));
		
		//perform convolution in x and y directions
		for (int x = initX; x < maxX; x++) {
			for (int y = initY; y < maxY; y += width) {
				int index = x + y;
				float sumX = data[index] * kernel[0];
				float sumY = sumX;
				int xOffset = 1;
				int yOffset = width;
				for(; xOffset < kwidth ;) {
					sumY += kernel[xOffset] * (data[index - yOffset] + data[index + yOffset]);
					sumX += kernel[xOffset] * (data[index - xOffset] + data[index + xOffset]);
					yOffset += width;
					xOffset++;
				}
				
				yConv[index] = sumY;
				xConv[index] = sumX;
			}
 
		}
 
		for (int x = initX; x < maxX; x++) {
			for (int y = initY; y < maxY; y += width) {
				float sum = 0f;
				int index = x + y;
				for (int i = 1; i < kwidth; i++)
					sum += diffKernel[i] * (yConv[index - i] - yConv[index + i]);
 
				xGradient[index] = sum;
			}
 
		}

		for (int x = kwidth; x < width - kwidth; x++) {
			for (int y = initY; y < maxY; y += width) {
				float sum = 0.0f;
				int index = x + y;
				int yOffset = width;
				for (int i = 1; i < kwidth; i++) {
					sum += diffKernel[i] * (xConv[index - yOffset] - xConv[index + yOffset]);
					yOffset += width;
				}
 
				yGradient[index] = sum;
			}
 
		}

 
		initX = kwidth;
		maxX = width - kwidth;
		initY = width * kwidth;
		maxY = width * (height - kwidth);
		for (int x = initX; x < maxX; x++) {
			for (int y = initY; y < maxY; y += width) {
				int index = x + y;
				int indexN = index - width;
				int indexS = index + width;
				int indexW = index - 1;
				int indexE = index + 1;
				int indexNW = indexN - 1;
				int indexNE = indexN + 1;
				int indexSW = indexS - 1;
				int indexSE = indexS + 1;
				
				float xGrad = xGradient[index];
				float yGrad = yGradient[index];
				float gradMag = hypot(xGrad, yGrad);
				if(!keepOnlyMaxima)magnitude[index]=(int) Math.round(MAGNITUDE_SCALE * gradMag);
				else {
					//perform non-maximal supression
					float nMag = hypot(xGradient[indexN], yGradient[indexN]);
					float sMag = hypot(xGradient[indexS], yGradient[indexS]);
					float wMag = hypot(xGradient[indexW], yGradient[indexW]);
					float eMag = hypot(xGradient[indexE], yGradient[indexE]);
					float neMag = hypot(xGradient[indexNE], yGradient[indexNE]);
					float seMag = hypot(xGradient[indexSE], yGradient[indexSE]);
					float swMag = hypot(xGradient[indexSW], yGradient[indexSW]);
					float nwMag = hypot(xGradient[indexNW], yGradient[indexNW]);
					float tmp;
					/*
					 * An explanation of what's happening here, for those who want
					 * to understand the source: This performs the "non-maximal
					 * supression" phase of the Canny edge detection in which we
					 * need to compare the gradient magnitude to that in the
					 * direction of the gradient; only if the value is a local
					 * maximum do we consider the point as an edge candidate.
					 * 
					 * We need to break the comparison into a number of different
					 * cases depending on the gradient direction so that the
					 * appropriate values can be used. To avoid computing the
					 * gradient direction, we use two simple comparisons: first we
					 * check that the partial derivatives have the same sign (1)
					 * and then we check which is larger (2). As a consequence, we
					 * have reduced the problem to one of four identical cases that
					 * each test the central gradient magnitude against the values at
					 * two points with 'identical support'; what this means is that
					 * the geometry required to accurately interpolate the magnitude
					 * of gradient function at those points has an identical
					 * geometry (upto right-angled-rotation/reflection).
					 * 
					 * When comparing the central gradient to the two interpolated
					 * values, we avoid performing any divisions by multiplying both
					 * sides of each inequality by the greater of the two partial
					 * derivatives. The common comparand is stored in a temporary
					 * variable (3) and reused in the mirror case (4).
					 * 
					 */
					if (xGrad * yGrad <= (float) 0 /*(1)*/
						? Math.abs(xGrad) >= Math.abs(yGrad) /*(2)*/
							? (tmp = Math.abs(xGrad * gradMag)) >= Math.abs(yGrad * neMag - (xGrad + yGrad) * eMag) /*(3)*/
								&& tmp > Math.abs(yGrad * swMag - (xGrad + yGrad) * wMag) /*(4)*/
							: (tmp = Math.abs(yGrad * gradMag)) >= Math.abs(xGrad * neMag - (yGrad + xGrad) * nMag) /*(3)*/
								&& tmp > Math.abs(xGrad * swMag - (yGrad + xGrad) * sMag) /*(4)*/
						: Math.abs(xGrad) >= Math.abs(yGrad) /*(2)*/
							? (tmp = Math.abs(xGrad * gradMag)) >= Math.abs(yGrad * seMag + (xGrad - yGrad) * eMag) /*(3)*/
								&& tmp > Math.abs(yGrad * nwMag + (xGrad - yGrad) * wMag) /*(4)*/
							: (tmp = Math.abs(yGrad * gradMag)) >= Math.abs(xGrad * seMag + (yGrad - xGrad) * sMag) /*(3)*/
								&& tmp > Math.abs(xGrad * nwMag + (yGrad - xGrad) * nMag) /*(4)*/
						) {
						magnitude[index] = gradMag >= MAGNITUDE_LIMIT ? MAGNITUDE_MAX : (int) (MAGNITUDE_SCALE * gradMag);
						//NOTE: The orientation of the edge is not employed by this
						//implementation. It is a simple matter to compute it at
						//this point as: Math.atan2(yGrad, xGrad);
					} else {
						magnitude[index] = 0;
					}
				}
			}
		}
	}
 
	//NOTE: It is quite feasible to replace the implementation of this method
	//with one which only loosely approximates the hypot function. I've tested
	//simple approximations such as Math.abs(x) + Math.abs(y) and they work fine.
	public static float hypot(float x, float y) {
		return (float) Math.hypot(x, y);
	}
 
	public static float gaussian(float x, float sigma) {
		return (float) Math.exp(-(x * x) / (2f * sigma * sigma));
	}
	
	
	
	
	
	public static ImagePlus drawRectangleInImage(ImagePlus imgIn,int x0,int y0,int xf,int yf,int value) {
		if(imgIn.getType() != ImagePlus.GRAY8)return imgIn;
		ImagePlus img=new Duplicator().run(imgIn);
		int xM=img.getWidth();
		int yM=img.getHeight();
		int zM=img.getStackSize();
		byte[][] valsImg=new byte[zM][];
		for(int z=0;z<zM;z++) {
			valsImg[z]=(byte [])img.getStack().getProcessor(z+1).getPixels();
			for(int x=x0;x<=xf;x++) {
				for(int y=y0;y<=yf;y++) {
					valsImg[z][xM*y+x]=  (byte)( ((byte)value) & 0xff);
				}
			}
		}			
		return img;
	}

	public static ImagePlus drawParallepipedInImage(ImagePlus imgIn,int x0,int y0,int z0,int xf,int yf,int zf,int value) {
		if(imgIn.getType() != ImagePlus.GRAY8)return imgIn;
		ImagePlus img=new Duplicator().run(imgIn);
		int xM=img.getWidth();
		int yM=img.getHeight();
		int zM=img.getStackSize();
		byte[][] valsImg=new byte[zM][];
		for(int z=z0;z<=zf;z++) {
			valsImg[z]=(byte [])img.getStack().getProcessor(z+1).getPixels();
			for(int x=x0;x<=xf;x++) {
				for(int y=y0;y<=yf;y++) {
					valsImg[z][xM*y+x]=  (byte)( ((byte)value) & 0xff);
				}
			}
		}			
		return img;
	}

	
	public static void printDebugIntro(int N,String message) {
		for(int i=0;i<N;i++)System.out.println("################## DEBUG ###########");
		System.out.println(message);
	}

	
	public static ImagePlus drawCylinderInImage(ImagePlus imgIn,double ray,int x0,int y0,int value) {
		if(imgIn.getType() != ImagePlus.GRAY8)return imgIn;
		ImagePlus img=new Duplicator().run(imgIn);
		VitimageUtils.adjustImageCalibration(img, imgIn);
		int xM=img.getWidth();
		int yM=img.getHeight();
		int zM=img.getStackSize();
		double voxSX=img.getCalibration().pixelWidth;
		double voxSY=img.getCalibration().pixelHeight;
		double realDisX;
		double realDisY;
		double realDisZ;
		byte[][] valsImg=new byte[zM][];
		double distance;
		for(int z=0;z<zM;z++) {
			valsImg[z]=(byte [])img.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<xM;x++) {
				for(int y=0;y<yM;y++) {
					realDisX=(x-x0)*voxSX;
					realDisY=(y-y0)*voxSY;
					distance=Math.sqrt( realDisX * realDisX  +  realDisY * realDisY );
					if(distance < ray) {
						valsImg[z][xM*y+x]=  (byte)( value & 0xff);
					}
				}
			}			
		}
		return img;
	}

	
	
	public static ImagePlus drawCircleInImage(ImagePlus imgIn,double ray,int x0,int y0,int z0,int value) {
		if(imgIn.getType() != ImagePlus.GRAY8)return imgIn;
		ImagePlus img=new Duplicator().run(imgIn);
		VitimageUtils.adjustImageCalibration(img, imgIn);
		int xM=img.getWidth();
		int yM=img.getHeight();
		int zM=img.getStackSize();
		double voxSX=img.getCalibration().pixelWidth;
		double voxSY=img.getCalibration().pixelHeight;
		double voxSZ=img.getCalibration().pixelDepth;
		double realDisX;
		double realDisY;
		double realDisZ;
		byte[][] valsImg=new byte[zM][];
		double distance;
		for(int z=0;z<zM;z++) {
			valsImg[z]=(byte [])img.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<xM;x++) {
				for(int y=0;y<yM;y++) {
					realDisX=(x-x0)*voxSX;
					realDisY=(y-y0)*voxSY;
					realDisZ=(z-z0)*voxSZ;
					distance=Math.sqrt( realDisX * realDisX  +  realDisY * realDisY  + realDisZ * realDisZ  );
					if(distance < ray) {
						valsImg[z][xM*y+x]=  (byte)( value & 0xff);
					}
				}
			}			
		}
		return img;
	}

	public static ImagePlus drawPointAtPixelCoordinatesInImageModifyingIt(ImagePlus imgIn,int x0,int y0,int z0,int value) {
		if(imgIn.getType() != ImagePlus.GRAY8)return imgIn;
		int xM=imgIn.getWidth();
		byte[] valsImg=(byte[])imgIn.getStack().getProcessor(z0+1).getPixels();
		valsImg[xM*y0+x0]=  (byte)( value & 0xff);
		return imgIn;
	}


	
	
	public static ImagePlus drawThickLineInFloatImage(ImagePlus imgIn,double ray,int x0,int y0,int z0,double[]vectZ,double value) {
		if(imgIn.getType() != ImagePlus.GRAY32)return null;
		ImagePlus img=new Duplicator().run(imgIn);
		int xM=img.getWidth();
		int yM=img.getHeight();
		int zM=img.getStackSize();
		double voxSX=img.getCalibration().pixelWidth;
		double voxSY=img.getCalibration().pixelHeight;
		double voxSZ=img.getCalibration().pixelDepth;
		double[]vectCur;
		double distanceLine;
		float[][] valsImg=new float[zM][];
		double distance;
		int hit=0;
		for(int z=0;z<zM;z++) {
			valsImg[z]=(float [])img.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<xM;x++) {
				for(int y=0;y<yM;y++) {
					vectCur=new double[] {(x-x0)*voxSX,(y-y0)*voxSY,(z-z0)*voxSZ};
					if(++hit %197000000==0) {
						System.out.println("Debug");
						double[]vectorProj=TransformUtils.proj_u_of_v( vectZ,vectCur);
						double[]difference=TransformUtils.vectorialSubstraction(vectCur,TransformUtils.proj_u_of_v(vectZ, vectCur));
						double norme=TransformUtils.norm(TransformUtils.vectorialSubstraction(vectCur,TransformUtils.proj_u_of_v(vectZ, vectCur)));
						System.out.println(TransformUtils.stringVector(vectZ, "vectZ"));
						System.out.println("coords point="+x+","+y+","+z);
						System.out.println("origine="+x0+","+y0+","+z0);
						System.out.println(TransformUtils.stringVector(vectCur, "vectCur"));
						System.out.println(TransformUtils.stringVector(vectorProj, "vectorProj"));
						System.out.println(TransformUtils.stringVector(difference, "difference"));						
						System.out.println("Norme = "+norme);						
					}

						
					distanceLine=TransformUtils.norm(TransformUtils.vectorialSubstraction(vectCur,TransformUtils.proj_u_of_v(vectZ, vectCur)));
					if(distanceLine < ray) {
						valsImg[z][xM*y+x]=  (float)(value);
					}
				}
			}			
		}
		return img;
	}

	
	
	
	public static ImagePlus drawThickLineInImage(ImagePlus imgIn,double ray,int x0,int y0,int z0,double[]vectZ) {
		if(imgIn.getType() == ImagePlus.GRAY32)return drawThickLineInFloatImage(imgIn,ray,x0,y0,z0,vectZ,255);
		if(imgIn.getType() == ImagePlus.GRAY16)return drawThickLineInShortImage(imgIn,ray,x0,y0,z0,vectZ);
		if(imgIn.getType() != ImagePlus.GRAY8)return null;
		ImagePlus img=new Duplicator().run(imgIn);
		int xM=img.getWidth();
		int yM=img.getHeight();
		int zM=img.getStackSize();
		double voxSX=img.getCalibration().pixelWidth;
		double voxSY=img.getCalibration().pixelHeight;
		double voxSZ=img.getCalibration().pixelDepth;
		double[]vectCur;
		double distanceLine;
		byte[][] valsImg=new byte[zM][];
		double distance;
		int hit=0;
		for(int z=0;z<zM;z++) {
			valsImg[z]=(byte [])img.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<xM;x++) {
				for(int y=0;y<yM;y++) {
					vectCur=new double[] {(x-x0)*voxSX,(y-y0)*voxSY,(z-z0)*voxSZ};
					if(++hit %197000000==0) {
						System.out.println("Debug");
						double[]vectorProj=TransformUtils.proj_u_of_v( vectZ,vectCur);
						double[]difference=TransformUtils.vectorialSubstraction(vectCur,TransformUtils.proj_u_of_v(vectZ, vectCur));
						double norme=TransformUtils.norm(TransformUtils.vectorialSubstraction(vectCur,TransformUtils.proj_u_of_v(vectZ, vectCur)));
						System.out.println(TransformUtils.stringVector(vectZ, "vectZ"));
						System.out.println("coords point="+x+","+y+","+z);
						System.out.println("origine="+x0+","+y0+","+z0);
						System.out.println(TransformUtils.stringVector(vectCur, "vectCur"));
						System.out.println(TransformUtils.stringVector(vectorProj, "vectorProj"));
						System.out.println(TransformUtils.stringVector(difference, "difference"));						
						System.out.println("Norme = "+norme);						
					}

						
					distanceLine=TransformUtils.norm(TransformUtils.vectorialSubstraction(vectCur,TransformUtils.proj_u_of_v(vectZ, vectCur)));
					if(distanceLine < ray) {
						valsImg[z][xM*y+x]=  (byte)( 255 & 0xff);
					}
				}
			}			
		}
		return img;
	}

	public static ImagePlus drawThickLineInShortImage(ImagePlus imgIn,double ray,int x0,int y0,int z0,double[]vectZ) {
		if(imgIn.getType() != ImagePlus.GRAY16)return null;
		ImagePlus img=new Duplicator().run(imgIn);
		int xM=img.getWidth();
		int yM=img.getHeight();
		int zM=img.getStackSize();
		double voxSX=img.getCalibration().pixelWidth;
		double voxSY=img.getCalibration().pixelHeight;
		double voxSZ=img.getCalibration().pixelDepth;
		double[]vectCur;
		double distanceLine;
		short[][] valsImg=new short[zM][];
		double distance;
		int hit=0;
		for(int z=0;z<zM;z++) {
			valsImg[z]=(short [])img.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<xM;x++) {
				for(int y=0;y<yM;y++) {
					vectCur=new double[] {(x-x0)*voxSX,(y-y0)*voxSY,(z-z0)*voxSZ};
					if(++hit %197000000==0) {
						System.out.println("Debug");
						double[]vectorProj=TransformUtils.proj_u_of_v( vectZ,vectCur);
						double[]difference=TransformUtils.vectorialSubstraction(vectCur,TransformUtils.proj_u_of_v(vectZ, vectCur));
						double norme=TransformUtils.norm(TransformUtils.vectorialSubstraction(vectCur,TransformUtils.proj_u_of_v(vectZ, vectCur)));
						System.out.println(TransformUtils.stringVector(vectZ, "vectZ"));
						System.out.println("coords point="+x+","+y+","+z);
						System.out.println("origine="+x0+","+y0+","+z0);
						System.out.println(TransformUtils.stringVector(vectCur, "vectCur"));
						System.out.println(TransformUtils.stringVector(vectorProj, "vectorProj"));
						System.out.println(TransformUtils.stringVector(difference, "difference"));						
						System.out.println("Norme = "+norme);						
					}

						
					distanceLine=TransformUtils.norm(TransformUtils.vectorialSubstraction(vectCur,TransformUtils.proj_u_of_v(vectZ, vectCur)));
					if(distanceLine < ray) {
						valsImg[z][xM*y+x]=  (short)( 255 & 0xffff);
					}
				}
			}			
		}
		return img;
	}

	
	
	
	public static ImagePlus eraseBorder(ImagePlus imgIn) {
		if(imgIn.getType() != ImagePlus.GRAY8)return imgIn;
		ImagePlus img=new Duplicator().run(imgIn);
		int xM=img.getWidth();
		int yM=img.getHeight();
		int zM=img.getStackSize();
		byte[]valsImg;
		for(int z=0;z<zM;z++) {
			valsImg=(byte [])img.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<xM;x++) {
				for(int y=0;y<yM;y++) {
					if( (x==0) || (x==xM-1) || (y==0) || (y==yM-1) || (z==0) || (z==zM-1) ) { 	
						valsImg[xM*y+x]=  (byte)(0 & 0xffff);
					}
				}
			}			
		}
		return img;
	}
	
	public static boolean isNullImage(ImagePlus imgIn) {
		if(imgIn.getType() == ImagePlus.GRAY16)return isNullShortImage(imgIn);
		if(imgIn.getType() == ImagePlus.GRAY32)return isNullFloatImage(imgIn);
		ImagePlus img=new Duplicator().run(imgIn);
		int xM=img.getWidth();
		int yM=img.getHeight();
		int zM=img.getStackSize();
		double voxSX=img.getCalibration().pixelWidth;
		double voxSY=img.getCalibration().pixelHeight;
		double voxSZ=img.getCalibration().pixelDepth;
		int hit=0;
		byte[]valsImg;
		for(int z=0;z<zM;z++) {
			valsImg=(byte [])img.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<xM;x++) {
				for(int y=0;y<yM;y++) {
					if ((valsImg[xM*y+x] & 0xff )> 0)hit++; 
				}
			}			
		}
		return (hit<1);
	}

	
	public static double maxOfImage(ImagePlus img) {
		double max=0;
		int xM=img.getWidth();
		int yM=img.getHeight();
		int zM=img.getStackSize();
		if(img.getType() == ImagePlus.GRAY8) {
			byte[]valsImg;
			for(int z=0;z<zM;z++) {
				valsImg=(byte [])img.getStack().getProcessor(z+1).getPixels();
				for(int x=0;x<xM;x++) {
					for(int y=0;y<yM;y++) {
						if ((valsImg[xM*y+x] & 0xff )> max)max=(double)(valsImg[xM*y+x] & 0xff); 
					}
				}			
			}
		}
		if(img.getType() == ImagePlus.GRAY16) {
			short[]valsImg;
			for(int z=0;z<zM;z++) {
				valsImg=(short [])img.getStack().getProcessor(z+1).getPixels();
				for(int x=0;x<xM;x++) {
					for(int y=0;y<yM;y++) {
						if ((valsImg[xM*y+x] & 0xffff )> max)max=(double)(valsImg[xM*y+x] & 0xffff); 
					}
				}			
			}
		}
		if(img.getType() == ImagePlus.GRAY32) {
			float[]valsImg;
			for(int z=0;z<zM;z++) {
				valsImg=(float [])img.getStack().getProcessor(z+1).getPixels();
				for(int x=0;x<xM;x++) {
					for(int y=0;y<yM;y++) {
						if ((valsImg[xM*y+x])> max)max=(double)(valsImg[xM*y+x]); 
					}
				}			
			}
		}
		return max;	
	}

	
	public static boolean isNullShortImage(ImagePlus imgIn) {
		if(imgIn.getType() != ImagePlus.GRAY16)return false;
		ImagePlus img=new Duplicator().run(imgIn);
		int xM=img.getWidth();
		int yM=img.getHeight();
		int zM=img.getStackSize();
		double voxSX=img.getCalibration().pixelWidth;
		double voxSY=img.getCalibration().pixelHeight;
		double voxSZ=img.getCalibration().pixelDepth;
		int hit=0;
		short[]valsImg;
		for(int z=0;z<zM;z++) {
			valsImg=(short [])img.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<xM;x++) {
				for(int y=0;y<yM;y++) {
					if (valsImg[xM*y+x] > 0) {
						hit++;
					//	System.out.println("Valeur supérieure :"+valsImg[xM*y+x]);hit++; 
					}
				}
			}			
		}
		//System.out.println("Hits="+hit);
		return (hit<1);
	}
	

	public static boolean isNullFloatImage(ImagePlus imgIn) {
		double epsilon=10E-10;
		if(imgIn.getType() != ImagePlus.GRAY32)return false;
		ImagePlus img=new Duplicator().run(imgIn);
		int xM=img.getWidth();
		int yM=img.getHeight();
		int zM=img.getStackSize();
		double voxSX=img.getCalibration().pixelWidth;
		double voxSY=img.getCalibration().pixelHeight;
		double voxSZ=img.getCalibration().pixelDepth;
		int hit=0;
		float[]valsImg;
		for(int z=0;z<zM;z++) {
			valsImg=(float [])img.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<xM;x++) {
				for(int y=0;y<yM;y++) {
					if (Math.abs(valsImg[xM*y+x]) > epsilon) {
						hit++;
					//	System.out.println("Valeur supérieure :"+valsImg[xM*y+x]);hit++; 
					}
				}
			}			
		}
		//System.out.println("Hits="+hit);
		return (hit<1);
	}

	
	
	
	public static ImagePlus getBinaryMask(ImagePlus img,double threshold) {
		int dimX=img.getWidth(); int dimY=img.getHeight(); int dimZ=img.getStackSize();
		int type=(img.getType()==ImagePlus.GRAY8 ? 8 : img.getType()==ImagePlus.GRAY16 ? 16 : img.getType()==ImagePlus.GRAY32 ? 32 : 24);
		ImagePlus ret=IJ.createImage("", dimX, dimY, dimZ, 8);
		VitimageUtils.adjustImageCalibration(ret,img);
		if(type==8) {
			for(int z=0;z<dimZ;z++) {
				byte []tabImg=(byte[])img.getStack().getProcessor(z+1).getPixels();
				byte []tabRet=(byte[])ret.getStack().getProcessor(z+1).getPixels();
				for(int x=0;x<dimX;x++) {
					for(int y=0;y<dimY;y++) {
						if( (tabImg[dimX*y+x] & 0xff) >= (byte)(((int)Math.round(threshold)) & 0xff)  )tabRet[dimX*y+x]=(byte)(255 & 0xff);
						else tabRet[dimX*y+x]=(byte)(0 & 0xff);
					}
				}
			}
		}
		else if(type==16) {
			for(int z=0;z<dimZ;z++) {
				short []tabImg=(short[])img.getStack().getProcessor(z+1).getPixels();
				byte []tabRet=(byte[])ret.getStack().getProcessor(z+1).getPixels();
				for(int x=0;x<dimX;x++) {
					for(int y=0;y<dimY;y++) {
						if( (tabImg[dimX*y+x] & 0xffff) >= (short)(((int)Math.round(threshold)) & 0xffff)  )tabRet[dimX*y+x]=(byte)(255 & 0xff);
						else tabRet[dimX*y+x]=(byte)(0 & 0xff);
					}
				}
			}
		}
		else if(type==32) {
			for(int z=0;z<dimZ;z++) {
				float []tabImg=(float[])img.getStack().getProcessor(z+1).getPixels();
				byte []tabRet=(byte[])ret.getStack().getProcessor(z+1).getPixels();
				for(int x=0;x<dimX;x++) {
					for(int y=0;y<dimY;y++) {
						if( (tabImg[dimX*y+x]) >= threshold )tabRet[dimX*y+x]=(byte)(255 & 0xff);
						else tabRet[dimX*y+x]=(byte)(0 & 0xff);
					}
				}
			}
		}
		else VitiDialogs.notYet("getBinary Mask type "+type);
		return ret;
	}

	public static ImagePlus getBinaryGrid(ImagePlus img,int pixelSpacing) {
		return getBinaryGrid(img, pixelSpacing,true,false);
	}
	
	public static ImagePlus getBinaryGrid(ImagePlus img,int pixelSpacing,boolean doubleSizeEveryFive,boolean displayTextBorder) {
		boolean doDouble=false;
		if(pixelSpacing<2)pixelSpacing=2;
		if(pixelSpacing>5)doDouble=true;
		int dimX=img.getWidth(); int dimY=img.getHeight(); int dimZ=img.getStackSize();
		ImagePlus ret=IJ.createImage("", dimX, dimY, dimZ, 8);
		VitimageUtils.adjustImageCalibration(ret,img);
		for(int z=0;z<dimZ;z++) {
			byte []tabRet=(byte[])ret.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<dimX;x++) {
				for(int y=0;y<dimY;y++) {
					if( (x%pixelSpacing==pixelSpacing/2) || 
					    (y%pixelSpacing==pixelSpacing/2)  ){
						tabRet[dimX*y+x]=(byte)(255 & 0xff);
					}
					if( (doubleSizeEveryFive || doDouble) && (x%pixelSpacing==pixelSpacing/2+1) || 
						    (y%pixelSpacing==pixelSpacing/2+1)){
							tabRet[dimX*y+x]=(byte)(255 & 0xff);
					}	
					if( doubleSizeEveryFive && ( (x%pixelSpacing==pixelSpacing/2+2) && ((x/pixelSpacing)%5==0)) || 
						    ((y%pixelSpacing==pixelSpacing/2+2) &&  ((y/pixelSpacing)%5==0))) {
							tabRet[dimX*y+x]=(byte)(255 & 0xff);
					}	
					if( pixelSpacing>9 && doubleSizeEveryFive && ( (x%pixelSpacing==pixelSpacing/2+3) && ((x/pixelSpacing)%5==0)) || 
						    ((y%pixelSpacing==pixelSpacing/2+3) &&  ((y/pixelSpacing)%5==0))) {
							tabRet[dimX*y+x]=(byte)(255 & 0xff);
					}	
				}
			}
		}	
		if(displayTextBorder) {
			int fontSize=(dimX+dimY+dimZ)/20;
			//On XY slices, Write X+, X- , Y+ and Y- 
			ret=VitimageUtils.writeTextOnImage("Y-", ret,fontSize,1,EPSILON,0.4,0.05);
			ret=VitimageUtils.writeTextOnImage("Y+", ret,fontSize,1,EPSILON,0.4,0.9);
			ret=VitimageUtils.writeTextOnImage("X-", ret,fontSize,1,EPSILON,0.05,0.4);
			ret=VitimageUtils.writeTextOnImage("X+", ret,fontSize,1,EPSILON,0.9,0.4);
			//On XZ slices, Write X+, X- , Z+ and Z- 
			ret=VitimageUtils.switchAxis(ret, 2);//switch Y and Z
			ret=VitimageUtils.writeTextOnImage("Z-", ret,fontSize,1,EPSILON,0.4,0.05);
			ret=VitimageUtils.writeTextOnImage("Z+", ret,fontSize,1,EPSILON,0.4,0.9);
			ret=VitimageUtils.writeTextOnImage("X-", ret,fontSize,1,EPSILON,0.05,0.4);
			ret=VitimageUtils.writeTextOnImage("X+", ret,fontSize,1,EPSILON,0.9,0.4);
			ret=VitimageUtils.switchAxis(ret, 2);//switch Y and Z
			//On YZ slices, Write Y+, Y- , Z+ and Z- 
			ret=VitimageUtils.switchAxis(ret, 1);//switch X and Z
			ret=VitimageUtils.writeTextOnImage("Y-", ret,fontSize,1,EPSILON,0.4,0.05);
			ret=VitimageUtils.writeTextOnImage("Y+", ret,fontSize,1,EPSILON,0.4,0.9);
			ret=VitimageUtils.writeTextOnImage("Z-", ret,fontSize,1,EPSILON,0.05,0.4);
			ret=VitimageUtils.writeTextOnImage("Z+", ret,fontSize,1,EPSILON,0.9,0.4);
			ret=VitimageUtils.switchAxis(ret, 1);//switch Y and Z
		}

		return ret;
	}
	 
	public static double[][]getHistogram(ImagePlus img,int nbSlicesConsidered){
		if(img.getType()==ImagePlus.GRAY32 || img.getType()==ImagePlus.COLOR_256 || img.getType()==ImagePlus.COLOR_RGB){
			VitiDialogs.notYet("GetHistogram of float image in VitimageUtils");
			System.exit(0);
		}
		int nBins=img.getStack().getProcessor(1).getHistogram().length;
		double[]histo=new double[nBins];
		double[]histoCumul=new double[nBins];
		int[][]dataHisto=new int[nbSlicesConsidered][nBins];
		for(int n=1;n<nbSlicesConsidered+1;n++) {
			int index=1+(img.getStackSize()*n)/(nbSlicesConsidered+1);
			dataHisto[n-1]=img.getStack().getProcessor(index).getHistogram();
			System.out.println("Get histo : "+TransformUtils.stringVectorN(dataHisto[n-1], ""));
		}
		double sum=0;
		for(int b=0;b<nBins;b++) {
			for(int n=0;n<nbSlicesConsidered;n++) histo[b]+=dataHisto[n][b];
			histoCumul[b]=histo[b]+(b!=0 ? histo[b-1] : 0);
		}
		sum=histoCumul[nBins-1];
		for(int b=0;b<nBins;b++) {histo[b]/=sum;histoCumul[b]/=sum;}
		return new double[][] {histo,histoCumul};
	}
	
	public static int[]getRange(ImagePlus img,double percentageDynamicCovered,int nbSlicesConsidered){
		double firstWing=(100-percentageDynamicCovered)/2;
		double secondWing=percentageDynamicCovered+firstWing;
		firstWing/=100;
		secondWing/=100;
		double[][]histos=getHistogram(img,nbSlicesConsidered);
		int nBins=histos[0].length;
		int firstIndex=0;int secondIndex=nBins-1;
		while((firstIndex<nBins) && (histos[1][firstIndex]<firstWing))firstIndex++;
		if(firstIndex>0)firstIndex--;
		while((secondIndex>=0) && (histos[1][secondIndex]>secondWing))secondIndex--;
		if(secondIndex<nBins-1)secondIndex++;
		System.out.println("Quantiles detectes : q1 a "+firstWing+" : index = "+firstIndex+" cumul="+histos[1][firstIndex]);
		System.out.println("Quantiles detectes : q2 a "+secondWing+" : index = "+secondIndex+" cumul="+histos[1][secondIndex]);
		System.out.println("");
		return new int[] {firstIndex,secondIndex};
	}
	
	
	public static double meanValueofImageAround(ImagePlus img,int x0,int y0,int z0,double ray) {
		int xMax=img.getWidth();
		int xm=(int)Math.round(x0-ray);
		int xM=(int)Math.round(x0+ray);
		int ym=(int)Math.round(y0-ray);
		int yM=(int)Math.round(y0+ray);
		if(z0<0)z0=0;
		if(z0>img.getStackSize()-1)z0=img.getStackSize()-1;

		if(xm<0)xm=0;
		if(ym<0)ym=0;
		if(xm>img.getWidth()-1)xm=img.getWidth()-1;
		if(ym>img.getHeight()-1)ym=img.getHeight()-1;

		if(xM<0)xM=0;
		if(yM<0)yM=0;
		if(xM>img.getWidth()-1)xM=img.getWidth()-1;
		if(yM>img.getHeight()-1)yM=img.getHeight()-1;
		double accumulator=0;
		double nbHits=0;
		if(img.getType() == ImagePlus.GRAY8) {
			byte[] valsImg=(byte [])img.getStack().getProcessor(z0+1).getPixels();
			for(int x=xm;x<=xM;x++) {
				for(int y=ym;y<=yM;y++) {
					accumulator+= (int)(  (  (byte)valsImg[xMax*y+x])  & 0xff);
					nbHits++;
				}
			}			
		}
		else if(img.getType() == ImagePlus.GRAY16) {
			short[] valsImg=(short[])img.getStack().getProcessor(z0+1).getPixels();
			for(int x=xm;x<=xM;x++) {
				for(int y=ym;y<=yM;y++) {
					accumulator+= (int)(  (  (short)valsImg[xMax*y+x])  & 0xffff);
					nbHits++;
				}
			}			
		}
		else if(img.getType() == ImagePlus.GRAY32) {
			float[] valsImg=(float[])img.getStack().getProcessor(z0+1).getPixels();
			for(int x=xm;x<=xM;x++) {
				for(int y=ym;y<=yM;y++) {
					accumulator+=(float)valsImg[xMax*y+x];
					nbHits++;
				}
			}			
		}
		if(nbHits==0)return 0;
		else return (accumulator/nbHits);
	}

	public static double []valuesOfImageAround(ImagePlus img,int x0,int y0,int z0,double ray) {
		int xMax=img.getWidth();
		int xm=(int)Math.round(x0-ray);
		int xM=(int)Math.round(x0+ray);
		int ym=(int)Math.round(y0-ray);
		int yM=(int)Math.round(y0+ray);
		if(z0<0)z0=0;
		if(z0>img.getStackSize()-1)z0=img.getStackSize()-1;

		if(xm<0)xm=0;
		if(ym<0)ym=0;
		if(xm>img.getWidth()-1)xm=img.getWidth()-1;
		if(ym>img.getHeight()-1)ym=img.getHeight()-1;

		if(xM<0)xM=0;
		if(yM<0)yM=0;
		if(xM>img.getWidth()-1)xM=img.getWidth()-1;
		if(yM>img.getHeight()-1)yM=img.getHeight()-1;

		int len=(xM-xm+1)*(yM-ym+1);
		int hit=0;
		double[] ret=new double[len];
		if(img.getType() == ImagePlus.GRAY8) {
			byte[] valsImg=(byte [])img.getStack().getProcessor(z0+1).getPixels();
			for(int x=xm;x<=xM;x++) {
				for(int y=ym;y<=yM;y++) {
						ret[hit++]= (int)(  (  (byte)valsImg[xMax*y+x])  & 0xff);
				}
			}			
		}
		else if(img.getType() == ImagePlus.GRAY16) {
			short[] valsImg=(short[])img.getStack().getProcessor(z0+1).getPixels();
			for(int x=xm;x<=xM;x++) {
				for(int y=ym;y<=yM;y++) {
						ret[hit++]= (int)(  (  (short)valsImg[xMax*y+x])  & 0xffff);
				}
			}			
		}
		else if(img.getType() == ImagePlus.GRAY32) {
			float[] valsImg=(float[])img.getStack().getProcessor(z0+1).getPixels();
			for(int x=xm;x<=xM;x++) {
				for(int y=ym;y<=yM;y++) {
						ret[hit++]=(  (  (float)valsImg[xMax*y+x]));
				}
			}			
		}
		return ret;
	}
	
	public static ImagePlus multiplyFloatImages(ImagePlus img1,ImagePlus img2) {
		if(img1.getStackSize()!=img2.getStackSize()) {
			IJ.log("Image dimensions does not match");return null;
		}
		else {
			return (new ImageCalculator().run("Multiply create 32-bit stack", img1, img2) );	
		}
	}

	
	public static int[][] getRoiAsCoords(Roi r) {
		int type=r.getType();
		int incr=0;
		if(type==0) {//Rectangle
			System.out.println("Roi de type "+type+" rectangle");
			final Rectangle rect = r.getBounds();
			final int x0 = rect.x;			final int y0 = rect.y;
			final int lastX = x0 + rect.width;  final int lastY = y0 + rect.height;
			int[][]ret=new int[rect.width*rect.height][2];
			for( int x = x0; x < lastX; x++ ) {
				for( int y = y0; y < lastY; y++ ){
					ret[incr++]=new int[] {x,y};
				}
			}	
			return ret;
		}
		else if(type==1 || type==2) {//Ovale
			System.out.println("Roi de type "+type+" ovale");
			final Rectangle rect = r.getBounds();
			final int x0 = rect.x;			final int y0 = rect.y;
			final int lastX = x0 + rect.width;			final int lastY = y0 + rect.height;
			for( int x = x0; x < lastX; x++ ) {				for( int y = y0; y < lastY; y++ ){					if(r.contains(x, y))					incr++;				}			}			
			int[][]ret=new int[incr][2];
			incr=0;
			for( int x = x0; x < lastX; x++ ) {				for( int y = y0; y < lastY; y++ ){					if(r.contains(x, y))					ret[incr++]=new int[] {x,y};				}			}			
			return ret;
		}
		else if(type>=3) {//Free
			System.out.println("Roi de type "+type+" other (freeline, shape, ...)");
			int[] x = r.getPolygon().xpoints;
			int[] y = r.getPolygon().ypoints;
			final int n = r.getPolygon().npoints;
			int[][]ret=new int[n][2];
			for( int ind = 0; ind < n; ind++ ) {
				ret[ind]=new int[] {x[ind],y[ind]};
			}	
			return ret;
		}
		else return null;
	}
	
	
	
	public static ImagePlus Sub222(ImagePlus img) {
		ResampleImageFilter res=new ResampleImageFilter();
		res.setDefaultPixelValue(0);
		res.setTransform(new ItkTransform());
		double []voxInit=VitimageUtils.getVoxelSizes(img);
		int []dimInit=VitimageUtils.getDimensions(img);
		for(int i=0;i<3;i++) {
			voxInit[i]*=2;
			dimInit[i]/=2;
		}
		res.setOutputSpacing(ItkImagePlusInterface.doubleArrayToVectorDouble(voxInit));
		res.setSize(ItkImagePlusInterface.intArrayToVectorUInt32(dimInit));
		return ItkImagePlusInterface.itkImageToImagePlus(res.execute(ItkImagePlusInterface.imagePlusToItkImage(img)));
	}

	
	public static ImagePlus subXYZ(ImagePlus imgIn,double[]factors,boolean bilinear) {
		System.out.print("Subsampling XYZ with factors "+TransformUtils.stringVector(factors,""));
		ImagePlus out=VitimageUtils.imageCopy(imgIn);
		int []dimsNew=VitimageUtils.getDimensions(imgIn);
		System.out.print(TransformUtils.stringVector(dimsNew,"Dims before"));
		for(int d=0;d<3;d++) {
			dimsNew[d]=(int)(Math.ceil( dimsNew[d]*factors[d] ));
		}
		System.out.print(TransformUtils.stringVector(dimsNew,"Dims target"));
//		out.show();
		out.setTitle("Temp for resizing");
		System.out.println("\nScale..."+", "+"x="+factors[0]+" y="+factors[1]+" z="+factors[2]+" width="+dimsNew[0]+" height="+dimsNew[1]+" depth="+dimsNew[2]+" interpolation="+(bilinear ?"Bilinear average":"None")+" process create");
		IJ.run(out, "Scale...", "x="+factors[0]+" y="+factors[1]+" z="+factors[2]+" width="+dimsNew[0]+" height="+dimsNew[1]+" depth="+dimsNew[2]+" interpolation="+(bilinear ?"Bilinear average":"None")+" process create");
		out=IJ.getImage();
		out.hide();
		System.out.println();
		return out;
	}

	public static ImagePlus subXYZPerso(ImagePlus imgIn,double[]sizeFactors,boolean bilinear,int defaultValue) {
		System.out.print("Subsampling XYZ with factors "+TransformUtils.stringVector(sizeFactors,""));
		int []dimsNew=VitimageUtils.getDimensions(imgIn);
		double[]vox=VitimageUtils.getVoxelSizes(imgIn);

		int X=dimsNew[0];int Y=dimsNew[1];int Z=dimsNew[2];
		System.out.print(TransformUtils.stringVector(dimsNew,"Dims before"));
		for(int d=0;d<3;d++) {
			dimsNew[d]=(int)(Math.ceil( dimsNew[d]*sizeFactors[d] ));
			vox[d]/=sizeFactors[d];
		}
		System.out.print(TransformUtils.stringVector(dimsNew,"Dims target"));
		int Xout=dimsNew[0];int Yout=dimsNew[1];int Zout=dimsNew[2];
		
		double xx,yy,zz,dx,dy,dz,accumulatorVals,accumulatorPond;
		int x0,y0,z0;
		double[][][]factors=new double[2][2][2];
		if(imgIn.getType() == ImagePlus.GRAY8) {
			ImagePlus imgOut=IJ.createImage("", Xout, Yout,Zout,8);
//			VitimageUtils.printImageResume(imgOut,"imgOut");
			byte[][]valsIn=new byte[Z][];
			byte[][]valsOut=new byte[Zout][];
			for(int z=0;z<Z;z++)valsIn[z]=(byte[])imgIn.getStack().getProcessor(z+1).getPixels();
			for(int z=0;z<Zout;z++)valsOut[z]=(byte[])imgOut.getStack().getProcessor(z+1).getPixels();
			for(int z=0;z<Zout;z++) {
				for(int x=0;x<Xout;x++) {
					for(int y=0;y<Yout;y++) {
						xx=x/sizeFactors[0];
						yy=y/sizeFactors[1];
						zz=z/sizeFactors[2];
						if(!bilinear) {
							x0=(int)Math.round(xx);
							y0=(int)Math.round(yy);
							z0=(int)Math.round(zz);
//							System.out.println("OUT "+x+","+y+","+z);
//							System.out.println("-> IN "+x0+","+y0+","+z0);
							if(x0>X-1 || y0>Y-1 || z0>Z-1) valsOut[z][Xout*y+x]=(byte)(0 & 0xff);
							else valsOut[z][Xout*y+x]=valsIn[z0][X*y0+x0];
						}
						else {
							x0=(int)Math.floor(xx);
							y0=(int)Math.floor(yy);
							z0=(int)Math.floor(zz);
							dx=xx-x0;
							dy=yy-y0;
							dz=zz-z0;
							factors[0][0][0]=(1-dx)*(1-dy)*(1-dz);
							factors[0][0][1]=(1-dx)*(1-dy)*(dz);
							factors[0][1][0]=(1-dx)*(dy)*(1-dz);
							factors[0][1][1]=(1-dx)*(dy)*(dz);
							factors[1][0][0]=(dx)*(1-dy)*(1-dz);
							factors[1][0][1]=(dx)*(1-dy)*(dz);
							factors[1][1][0]=(dx)*(dy)*(1-dz);
							factors[1][1][1]=(dx)*(dy)*(dz);
							accumulatorVals= accumulatorPond=0;
	
							for(int di=0;di<2;di++)for(int dj=0;dj<2;dj++)for(int dk=0;dk<2;dk++) {
								if(! ((x0+di)<0 || (y0+dj)<0 || (z0+dk)<0 || (x0+di)>X-1 || (y0+dj)>Y-1 || (z0+dk)>Z-1) ){
									accumulatorVals+=factors[di][dj][dk]*((int)((byte)(valsIn[(z0+dk)][X*(y0+dj)+(x0+di)] & 0xff)));
									accumulatorPond+=factors[di][dj][dk];
								}
							}
							accumulatorVals/=accumulatorPond;
							accumulatorVals=Math.max(Math.min(accumulatorVals, 255),0);
							valsOut[z][Xout*y+x]=(byte)(   ((int)(Math.round(accumulatorVals)) & 0xff ) );
						}
					}
				}			
			}
			VitimageUtils.adjustImageCalibration(imgOut, vox,"mm");
			return imgOut;
		}
		else if(imgIn.getType() == ImagePlus.GRAY16) {
			ImagePlus imgOut=IJ.createImage("", Xout, Yout,Zout,16);
			short[][]valsIn=new short[Z][];
			short[][]valsOut=new short[Zout][];
			for(int z=0;z<Z;z++)valsIn[z]=(short[])imgIn.getStack().getProcessor(z+1).getPixels();
			for(int z=0;z<Zout;z++)valsOut[z]=(short[])imgOut.getStack().getProcessor(z+1).getPixels();
			for(int z=0;z<Zout;z++) {
				for(int x=0;x<Xout;x++) {
					for(int y=0;y<Yout;y++) {
						xx=x/sizeFactors[0];
						yy=y/sizeFactors[1];
						zz=z/sizeFactors[2];
						if(!bilinear) {
							x0=(int)Math.round(xx);
							y0=(int)Math.round(yy);
							z0=(int)Math.round(zz);
							if(x0<0 || y<0 || z0<0 || x0>X-1 || y0>Y-1 || z0>Z-1) valsOut[z][Xout*y+x]=(short)(0 & 0xffff);
							else valsOut[z][Xout*y+x]=valsIn[z0][X*y0+x0];
						}
						else {
							x0=(int)Math.floor(xx);
							y0=(int)Math.floor(yy);
							z0=(int)Math.floor(zz);
							dx=xx-x0;
							dy=yy-y0;
							dz=zz-z0;
							factors[0][0][0]=(1-dx)*(1-dy)*(1-dz);
							factors[0][0][1]=(1-dx)*(1-dy)*(dz);
							factors[0][1][0]=(1-dx)*(dy)*(1-dz);
							factors[0][1][1]=(1-dx)*(dy)*(dz);
							factors[1][0][0]=(dx)*(1-dy)*(1-dz);
							factors[1][0][1]=(dx)*(1-dy)*(dz);
							factors[1][1][0]=(dx)*(dy)*(1-dz);
							factors[1][1][1]=(dx)*(dy)*(dz);
							accumulatorVals= accumulatorPond=0;
	
							for(int di=0;di<2;di++)for(int dj=0;dj<2;dj++)for(int dk=0;dk<2;dk++) {
								if((x0+di)<0 || (y0+dj)<0 || (z0+dk)<0 || (x0+di)>X-1 || (y0+dj)>Y-1 || (z0+dk)>Z-1) {
									accumulatorVals+=factors[di][dj][dk]*((int)((short)(valsIn[(z0+dk)][X*(y0+dj)+(x0+di)] & 0xffff)));
									accumulatorPond+=factors[di][dj][dk];
								}
							}
							accumulatorVals/=accumulatorPond;
							accumulatorVals=Math.max(Math.min(accumulatorVals, 256*256-1),0);
							valsOut[z][Xout*y+x]=(short)(   ((int)(Math.round(accumulatorVals)) & 0xffff ) );
						}
					}
				}			
			}
			VitimageUtils.adjustImageCalibration(imgOut, vox,"mm");
			return imgOut;
		}
		else if(imgIn.getType() == ImagePlus.GRAY32) {
			ImagePlus imgOut=IJ.createImage("", Xout, Yout,Zout,32);
			float[][]valsIn=new float[Z][];
			float[][]valsOut=new float[Zout][];
			for(int z=0;z<Z;z++)valsIn[z]=(float[])imgIn.getStack().getProcessor(z+1).getPixels();
			for(int z=0;z<Zout;z++)valsOut[z]=(float[])imgOut.getStack().getProcessor(z+1).getPixels();
			for(int z=0;z<Zout;z++) {
				for(int x=0;x<Xout;x++) {
					for(int y=0;y<Yout;y++) {
						xx=x/sizeFactors[0];
						yy=y/sizeFactors[1];
						zz=z/sizeFactors[2];
						if(!bilinear) {
							x0=(int)Math.round(xx);
							y0=(int)Math.round(yy);
							z0=(int)Math.round(zz);
							if(x0<0 || y<0 || z0<0 || x0>X-1 || y0>Y-1 || z0>Z-1) valsOut[z][Xout*y+x]=0;
							else valsOut[z][Xout*y+x]=valsIn[z0][X*y0+x0];
						}
						else {
							x0=(int)Math.floor(xx);
							y0=(int)Math.floor(yy);
							z0=(int)Math.floor(zz);
							dx=xx-x0;
							dy=yy-y0;
							dz=zz-z0;
							factors[0][0][0]=(1-dx)*(1-dy)*(1-dz);
							factors[0][0][1]=(1-dx)*(1-dy)*(dz);
							factors[0][1][0]=(1-dx)*(dy)*(1-dz);
							factors[0][1][1]=(1-dx)*(dy)*(dz);
							factors[1][0][0]=(dx)*(1-dy)*(1-dz);
							factors[1][0][1]=(dx)*(1-dy)*(dz);
							factors[1][1][0]=(dx)*(dy)*(1-dz);
							factors[1][1][1]=(dx)*(dy)*(dz);
							accumulatorVals= accumulatorPond=0;
	
							for(int di=0;di<2;di++)for(int dj=0;dj<2;dj++)for(int dk=0;dk<2;dk++) {
								if((x0+di)<0 || (y0+dj)<0 || (z0+dk)<0 || (x0+di)>X-1 || (y0+dj)>Y-1 || (z0+dk)>Z-1) {}
								else {
									//System.out.println("Coords in "+(x0+di)+" , "+(y0+dj)+" , "+(z0+dk));
									accumulatorVals+=factors[di][dj][dk]*(valsIn[(z0+dk)][X*(y0+dj)+(x0+di)]);
									accumulatorPond+=factors[di][dj][dk];
								}
							}
							valsOut[z][Xout*y+x]=(float)(accumulatorVals/accumulatorPond);
						}
					}
				}			
			}
			VitimageUtils.adjustImageCalibration(imgOut, vox,"mm");
			return imgOut;
		}
		else return null;
	}

	
	
	
	
	
	public static ImagePlus Up222(ImagePlus img) {
		ResampleImageFilter res=new ResampleImageFilter();
		res.setDefaultPixelValue(0);
		res.setTransform(new ItkTransform());
		double []voxInit=VitimageUtils.getVoxelSizes(img);
		int []dimInit=VitimageUtils.getDimensions(img);
		for(int i=0;i<3;i++) {
			voxInit[i]/=2;
			dimInit[i]*=2;
		}
		res.setOutputSpacing(ItkImagePlusInterface.doubleArrayToVectorDouble(voxInit));
		res.setSize(ItkImagePlusInterface.intArrayToVectorUInt32(dimInit));
		return ItkImagePlusInterface.itkImageToImagePlus(res.execute(ItkImagePlusInterface.imagePlusToItkImage(img)));
	}

	public static ItkTransform Up222Dense(ItkTransform tr) {
		ResampleImageFilter res=new ResampleImageFilter();
		res.setDefaultPixelValue(0);
		Image img=new DisplacementFieldTransform((org.itk.simple.Transform)(tr)).getDisplacementField();
		int []dimInit=ItkImagePlusInterface.vectorUInt32ToIntArray(img.getSize());
		double []voxInit=ItkImagePlusInterface.vectorDoubleToDoubleArray(img.getSpacing());
		for(int i=0;i<3;i++) {
			voxInit[i]/=2;
			dimInit[i]*=2;
		}
		res.setOutputSpacing(ItkImagePlusInterface.doubleArrayToVectorDouble(voxInit));
		res.setSize(ItkImagePlusInterface.intArrayToVectorUInt32(dimInit));
		return new ItkTransform(new DisplacementFieldTransform( res.execute ( img )));
	}

	public static ItkTransform Sub222Dense(ItkTransform tr) {
		ResampleImageFilter res=new ResampleImageFilter();
		res.setDefaultPixelValue(0);
		Image img=new DisplacementFieldTransform((org.itk.simple.Transform)(tr)).getDisplacementField();
		int []dimInit=ItkImagePlusInterface.vectorUInt32ToIntArray(img.getSize());
		double []voxInit=ItkImagePlusInterface.vectorDoubleToDoubleArray(img.getSpacing());
		for(int i=0;i<3;i++) {
			voxInit[i]*=2;
			dimInit[i]/=2;
		}
		res.setOutputSpacing(ItkImagePlusInterface.doubleArrayToVectorDouble(voxInit));
		res.setSize(ItkImagePlusInterface.intArrayToVectorUInt32(dimInit));
		return new ItkTransform(new DisplacementFieldTransform( res.execute ( img )));
	}
	

	
	
	public static double []valuesOfBlock(ImagePlus img,int xm,int ym,int zm,int xM,int yM,int zM) {
		int xMax=img.getWidth();
		if(zm<0)zm=0;
		if(zM>img.getStackSize()-1)zM=img.getStackSize()-1;
		if(xm<0)xm=0;
		if(ym<0)ym=0;
		if(xm>img.getWidth()-1)xm=img.getWidth()-1;
		if(ym>img.getHeight()-1)ym=img.getHeight()-1;
		if(xM<0)xM=0;
		if(yM<0)yM=0;
		if(xM>img.getWidth()-1)xM=img.getWidth()-1;
		if(yM>img.getHeight()-1)yM=img.getHeight()-1;

		int len=(xM-xm+1)*(yM-ym+1)*(zM-zm+1);
		int hit=0;
		double[] ret=new double[len];
		if(img.getType() == ImagePlus.GRAY8) {
			for(int z=zm;z<=zM;z++) {
				byte[] valsImg=(byte [])img.getStack().getProcessor(z+1).getPixels();
				for(int x=xm;x<=xM;x++) {
					for(int y=ym;y<=yM;y++) {
							ret[hit++]= (int)(  (  (byte)valsImg[xMax*y+x])  & 0xff);
					}
				}			
			}
		}
		else if(img.getType() == ImagePlus.GRAY16) {
			for(int z=zm;z<=zM;z++) {
				short[] valsImg=(short[])img.getStack().getProcessor(z+1).getPixels();
				for(int x=xm;x<=xM;x++) {
					for(int y=ym;y<=yM;y++) {
							ret[hit++]= (int)(  (  (short)valsImg[xMax*y+x])  & 0xffff);
					}
				}			
			}
		}
		else if(img.getType() == ImagePlus.GRAY32) {
			for(int z=zm;z<=zM;z++) {
				float[] valsImg=(float[])img.getStack().getProcessor(z+1).getPixels();
				for(int x=xm;x<=xM;x++) {
					for(int y=ym;y<=yM;y++) {
							ret[hit++]=(  (  (float)valsImg[xMax*y+x]));
					}
				}			
			}
		}
		return ret;
	}
	
	public static ImagePlus normalizeMeanAndVarianceAlongZ(ImagePlus imgTmp) {
		ImagePlus img=new Duplicator().run(imgTmp);
		int[]dims=VitimageUtils.getDimensions(img);
		double[][]stats=new double[dims[2]][2];
		int zMedUp=(dims[2]*6)/10;
		int zMedDown=(dims[2]*4)/10;
		for(int z=0;z<dims[2];z++) {
			if(z%30==0)System.out.print("  "+z+" / "+dims[2]);
			stats[z]=statistics1D(valuesOfBlockDouble(img,0,0,z,dims[0]-1,dims[1]-1,z));
		}
		System.out.println();
		int hits=0;
		double []statsZ=new double[2];
		for(int z=zMedDown;z<=zMedUp;z++) {
			statsZ[0]+=stats[z][0];
			statsZ[1]+=stats[z][1];
			hits++;
		}
		statsZ[0]/=hits;
		statsZ[1]/=hits;
		System.out.println("Moyenne et variance determinés : "+TransformUtils.stringVectorN(statsZ, ""));
		float[] valsImg;
		for(int z=0;z<dims[2];z++) {
			if(z%20==0) {
				System.out.print("  "+z+" / "+dims[2]);
			}
			valsImg=(float [])img.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<dims[0];x++) {
				for(int y=0;y<dims[1];y++) {
					valsImg[dims[0]*y+x]=(float) ((float)(  ( (valsImg[dims[0]*y+x]) - stats[z][0]  )/stats[z][1] ) * statsZ[1] + statsZ[0]);
				}
			}			
		}
		System.out.println();
		return img;
	}

	

	
	public static ImagePlus normalizeCapillaryMeanAlongZ(ImagePlus imgTmp) {
		VitiDialogs.notYet("Normalize capillary along Z");
		return null;
	}

	

	
	
	
	
	public static double []valuesOfBlockDoubleSlice(ImagePlus img,double xxm,double yym,double xxM,double yyM) {
		int xMax=img.getWidth();
		if(xxm<0)xxm=0;
		if(yym<0)yym=0;
		if(xxm>=img.getWidth()-1)xxm=img.getWidth()-2;
		if(yym>=img.getHeight()-1)yym=img.getHeight()-2;
		if(xxM<0)xxM=0;
		if(yyM<0)yyM=0;
		if(xxM>=img.getWidth()-1)xxM=img.getWidth()-2;
		if(yyM>=img.getHeight()-1)yyM=img.getHeight()-2;
		int len=(int)Math.round(xxM-xxm+1)*(int)Math.round(yyM-yym+1);

		int xm=(int)Math.floor(xxm);
		int ym=(int)Math.floor(yym);
		int xM=(int)Math.floor(xxM);
		int yM=(int)Math.floor(yyM);
		double xp=xxm-xm;
		double yp=yym-ym;
		double []factors= new double[]{ (1-xp)*(1-yp) ,  (xp)*(1-yp) , (1-xp)*(yp) ,     (xp)*(yp)  	};
		//System.out.println(TransformUtils.stringVectorN(factors,"factors"));
		//si xp=0 , 1 3 5 7 n influent pas
		//si xp=1 , 0 2 4 6 n influent pas
		//si yp=0 , 2 3 6 7 n influent pas
		//si yp=1 , 0 1 4 5 n influent pas
		//si zp=0 , 4 5 6 7 n influent pas
		//si zp=1 , 0 1 2 3 n influent pas	
		//    Z axis
		//     /|\ 6---------7         
		//      | /|        /|
		//      |/ |       / |  
		//      4---------5  |
		//      |  |      |  |
		//      |  2------|--3
		//      | /       | /
		//      |/        |/
		//      0---------1-----> X axis
		//
		int hit=0;
		double[] ret=new double[len];
		if(img.getType() == ImagePlus.GRAY8) {
			byte[]valsImg=(byte [])img.getStack().getProcessor(1).getPixels();
			for(int x=xm;x<=xM;x++) {
				for(int y=ym;y<=yM;y++) {
					//System.out.println("("+x+","+y+","+z+")");
					ret[hit++]= factors[0]*(int)(  (  (byte)valsImg[xMax*y+x])  & 0xff) + factors[1]*(int)(  (  (byte)valsImg[xMax*y+(x+1)])  & 0xff) +
								factors[2]*(int)(  (  (byte)valsImg[xMax*(y+1)+x])  & 0xff) + factors[3]*(int)(  (  (byte)valsImg[xMax*(y+1)+(x+1)])  & 0xff);
				}
			}			
		}
		else if(img.getType() == ImagePlus.GRAY16) {
			short[] valsImg=(short [])img.getStack().getProcessor(1).getPixels();
			for(int x=xm;x<=xM;x++) {
				for(int y=ym;y<=yM;y++) {
					ret[hit++]= factors[0]*(int)(  (  (short)valsImg[xMax*y+x])  & 0xffff) + factors[1]*(int)(  (  (short)valsImg[xMax*y+(x+1)])  & 0xffff) +
							factors[2]*(int)(  (  (short)valsImg[xMax*(y+1)+x])  & 0xffff) + factors[3]*(int)(  (  (short)valsImg[xMax*(y+1)+(x+1)])  & 0xffff);
				}
			}			
		}
		else if(img.getType() == ImagePlus.GRAY32) {
			float[] valsImg=(float [])img.getStack().getProcessor(1).getPixels();
			for(int x=xm;x<=xM;x++) {
				for(int y=ym;y<=yM;y++) {
					ret[hit++]= factors[0]*  (float)valsImg[xMax*y+x] + factors[1]* (float)valsImg[xMax*y+(x+1)] +
							factors[2]*(float)valsImg[xMax*(y+1)+x] + factors[3]*(float)valsImg[xMax*(y+1)+(x+1)] ;
				}
			}			
		}
		return ret;
	}
	
	public static ImagePlus switchValueInImage(ImagePlus img,int valueBefore, int valueAfter) {
		ImagePlus out=VitimageUtils.imageCopy(img);
		int xMax=img.getWidth();
		int yMax=img.getHeight();
		int zMax=img.getStackSize();
		int hit=0;
		if(img.getType() == ImagePlus.GRAY8) {
			for(int z=0;z<zMax;z++) {
				byte[]valsImg=(byte [])out.getStack().getProcessor(z+1).getPixels();
				for(int x=0;x<xMax;x++) {
					for(int y=0;y<yMax;y++) {
						if(  ((int) ( ((byte)valsImg[xMax*y+x])  & 0xff) ==valueBefore ) ) {
							hit++;
							valsImg[xMax*y+x]=(byte)( valueAfter &0xff);
						}
					}
				}			
			}
		}
		if(img.getType() == ImagePlus.GRAY16) {
			for(int z=0;z<zMax;z++) {
				short[]valsImg=(short [])out.getStack().getProcessor(z+1).getPixels();
				for(int x=0;x<xMax;x++) {
					for(int y=0;y<yMax;y++) {
						if(  ((int) ( ((short)valsImg[xMax*y+x])  & 0xffff) ==valueBefore ) ) {
							hit++;
							valsImg[xMax*y+x]=(short)( valueAfter &0xffff);
						}
					}
				}			
			}
		}
		return out;
	}
	
	
	
	
	public static double []valuesOfBlockDouble(ImagePlus img,double xxm,double yym,double zzm,double xxM,double yyM,double zzM) {
		int zImg=img.getStackSize();
		int xMax=img.getWidth();

		if(zzm<0)zzm=0;
		if(zzm>=img.getStackSize()-1)zzM=img.getStackSize()-2;
		if(zzM<0)zzm=0;
		if(zzM>=img.getStackSize()-1)zzM=img.getStackSize()-2;

		if(xxm<0)xxm=0;
		if(yym<0)yym=0;
		if(xxm>=img.getWidth()-1)xxm=img.getWidth()-2;
		if(yym>=img.getHeight()-1)yym=img.getHeight()-2;
		if(xxM<0)xxM=0;
		if(yyM<0)yyM=0;
		if(xxM>=img.getWidth()-1)xxM=img.getWidth()-2;
		if(yyM>=img.getHeight()-1)yyM=img.getHeight()-2;


		int len=(int)Math.round(xxM-xxm+1)*(int)Math.round(yyM-yym+1)*(int)Math.round(zzM-zzm+1);

		int xm=(int)Math.floor(xxm);
		int ym=(int)Math.floor(yym);
		int zm=(int)Math.floor(zzm);
		int xM=(int)Math.floor(xxM);
		int yM=(int)Math.floor(yyM);
		int zM=(int)Math.floor(zzM);
		double xp=xxm-xm;
		double yp=yym-ym;
		double zp=zzm-zm;
		double []factors= new double[]{ (1-xp)*(1-yp)*(1-zp) ,  (xp)*(1-yp)*(1-zp) , (1-xp)*(yp)*(1-zp) ,     (xp)*(yp)*(1-zp)  ,
									    (1-xp)*(1-yp)*(zp) ,  (xp)*(1-yp)*(zp) , (1-xp)*(yp)*(zp) ,     (xp)*(yp)*(zp) 	};
		//System.out.println(TransformUtils.stringVectorN(factors,"factors"));
		//si xp=0 , 1 3 5 7 n influent pas
		//si xp=1 , 0 2 4 6 n influent pas
		//si yp=0 , 2 3 6 7 n influent pas
		//si yp=1 , 0 1 4 5 n influent pas
		//si zp=0 , 4 5 6 7 n influent pas
		//si zp=1 , 0 1 2 3 n influent pas	
		//    Z axis
		//     /|\ 6---------7         
		//      | /|        /|
		//      |/ |       / |  
		//      4---------5  |
		//      |  |      |  |
		//      |  2------|--3
		//      | /       | /
		//      |/        |/
		//      0---------1-----> X axis
		//
		int hit=0;
		double[] ret=new double[len];
		if(img.getType() == ImagePlus.GRAY8) {
			byte[][] valsImg=new byte[zImg][];
			for(int z=0;z<zImg;z++)valsImg[z]=(byte [])img.getStack().getProcessor(z+1).getPixels();
			for(int z=zm;z<=zM;z++) {
				for(int x=xm;x<=xM;x++) {
					for(int y=ym;y<=yM;y++) {
						//System.out.println("("+x+","+y+","+z+")");
						ret[hit++]= factors[0]*(int)(  (  (byte)valsImg[z][xMax*y+x])  & 0xff) + factors[1]*(int)(  (  (byte)valsImg[z][xMax*y+(x+1)])  & 0xff) +
									factors[2]*(int)(  (  (byte)valsImg[z][xMax*(y+1)+x])  & 0xff) + factors[3]*(int)(  (  (byte)valsImg[z][xMax*(y+1)+(x+1)])  & 0xff) +
									factors[4]*(int)(  (  (byte)valsImg[z+1][xMax*y+x])  & 0xff) + factors[5]*(int)(  (  (byte)valsImg[z+1][xMax*y+(x+1)])  & 0xff) +
									factors[6]*(int)(  (  (byte)valsImg[z+1][xMax*(y+1)+x])  & 0xff) + factors[7]*(int)(  (  (byte)valsImg[z+1][xMax*(y+1)+(x+1)])  & 0xff) ;
					}
				}			
			}
		}
		else if(img.getType() == ImagePlus.GRAY16) {
			short[][] valsImg=new short[zImg][];
			for(int z=0;z<zImg;z++)valsImg[z]=(short [])img.getStack().getProcessor(z+1).getPixels();
			for(int z=zm;z<=zM;z++) {
				for(int x=xm;x<=xM;x++) {
					for(int y=ym;y<=yM;y++) {
						ret[hit++]= factors[0]*(int)(  (  (short)valsImg[z][xMax*y+x])  & 0xffff) + factors[1]*(int)(  (  (short)valsImg[z][xMax*y+(x+1)])  & 0xffff) +
								factors[2]*(int)(  (  (short)valsImg[z][xMax*(y+1)+x])  & 0xffff) + factors[3]*(int)(  (  (short)valsImg[z][xMax*(y+1)+(x+1)])  & 0xffff) +
								factors[4]*(int)(  (  (short)valsImg[z+1][xMax*y+x])  & 0xffff) + factors[5]*(int)(  (  (short)valsImg[z+1][xMax*y+(x+1)])  & 0xffff) +
								factors[6]*(int)(  (  (short)valsImg[z+1][xMax*(y+1)+x])  & 0xffff) + factors[7]*(int)(  (  (short)valsImg[z+1][xMax*(y+1)+(x+1)])  & 0xffff) ;
					}
				}			
			}
		}
		else if(img.getType() == ImagePlus.GRAY32) {
			float[][] valsImg=new float[zImg][];
			for(int z=0;z<zImg;z++)valsImg[z]=(float [])img.getStack().getProcessor(z+1).getPixels();
			for(int z=zm;z<=zM;z++) {
				for(int x=xm;x<=xM;x++) {
					for(int y=ym;y<=yM;y++) {
						ret[hit++]= factors[0]*  (float)valsImg[z][xMax*y+x] + factors[1]* (float)valsImg[z][xMax*y+(x+1)] +
								factors[2]*(float)valsImg[z][xMax*(y+1)+x] + factors[3]*(float)valsImg[z][xMax*(y+1)+(x+1)] +
								factors[4]*(float)valsImg[z+1][xMax*y+x] + factors[5]*(float)valsImg[z+1][xMax*y+(x+1)] +
								factors[6]*(float)valsImg[z+1][xMax*(y+1)+x] + factors[7]*(float)valsImg[z+1][xMax*(y+1)+(x+1)] ;
					}
				}			
			}
		}
		return ret;
	}
	
	
	public static ImagePlus setImageToValue(ImagePlus imgIn,double value) {
		if(imgIn.getType()==ImagePlus.GRAY32)return set32bitToValue(imgIn,value);
		if(imgIn.getType()==ImagePlus.GRAY16)return set16bitToValue(imgIn,(int)Math.round(value));
		if(imgIn.getType()==ImagePlus.GRAY8)return set8bitToValue(imgIn,(int)Math.round(value));
		return null;
	}
	
	
	
	public static ImagePlus set8bitToValue(ImagePlus imgIn,int value) {
		ImagePlus imgOut=new Duplicator().run(imgIn);
		for(int i=1;i<=imgIn.getStackSize();i++) {
			imgOut.getStack().getProcessor(i).set(value);
		}
		return imgOut;
	}
	
	public static ImagePlus set16bitToValue(ImagePlus imgIn,int value) {
		ImagePlus imgOut=new Duplicator().run(imgIn);
		for(int i=1;i<=imgIn.getStackSize();i++) {
			imgOut.getStack().getProcessor(i).set(value);
		}
		return imgOut;
	}

	public static ImagePlus set32bitToValue(ImagePlus imgIn,double value) {
		ImagePlus imgOut=new Duplicator().run(imgIn);
		for(int i=1;i<=imgIn.getStackSize();i++) {
			imgOut.getStack().getProcessor(i).set(value);
		}
		return imgOut;
	}

	public static double[] stdAndMeanValueofImageAround(ImagePlus img,int x0,int y0,int z0,double ray) {
		int xMax=img.getWidth();
		int xm=(int)Math.round(x0-ray);
		int xM=(int)Math.round(x0+ray);
		int ym=(int)Math.round(y0-ray);
		int yM=(int)Math.round(y0+ray);
		if(z0<0)z0=0;
		if(z0>img.getStackSize()-1)z0=img.getStackSize()-1;
		if(xm<0)xm=0;
		if(ym<0)ym=0;
		if(xm>img.getWidth()-1)xm=img.getWidth()-1;
		if(ym>img.getHeight()-1)ym=img.getHeight()-1;
		if(xM<0)xM=0;
		if(yM<0)yM=0;
		if(xM>img.getWidth()-1)xM=img.getWidth()-1;
		if(yM>img.getHeight()-1)yM=img.getHeight()-1;
		double mean=meanValueofImageAround(img,x0,y0,z0,ray);
		double accumulator=0;
		double nbHits=0;
		if(img.getType() == ImagePlus.GRAY8) {
			byte[] valsImg=(byte [])img.getStack().getProcessor(z0+1).getPixels();
			for(int x=xm;x<=xM;x++) {
				for(int y=ym;y<=yM;y++) {
					if( ((x-xM)*(x-xM)+(y-yM)*(y-yM)) < (ray*ray) ) {
						accumulator+= Math.pow( ((double)(  (int)(  (  (byte)valsImg[xMax*y+x])  & 0xff) )) - mean , 2);
						nbHits++;
					}			
				}
			}			
		}
		else if(img.getType() == ImagePlus.GRAY16) {
			short[] valsImg=(short[])img.getStack().getProcessor(z0+1).getPixels();
			for(int x=xm;x<=xM;x++) {
				for(int y=ym;y<=yM;y++) {
					if( ((x-xM)*(x-xM)+(y-yM)*(y-yM)) < (ray*ray) ) {
						accumulator+= Math.pow( ((double)(  (int)(  (  (short)valsImg[xMax*y+x])  & 0xffff) )) - mean , 2);
						nbHits++;
					}			
				}
			}			
		}
		else if(img.getType() == ImagePlus.GRAY32) {
			float[] valsImg=(float[])img.getStack().getProcessor(z0+1).getPixels();
			for(int x=xm;x<=xM;x++) {
				for(int y=ym;y<=yM;y++) {
					if( ((x-xM)*(x-xM)+(y-yM)*(y-yM)) < (ray*ray) ) {
						accumulator+=Math.pow(   (   (float)valsImg[xMax*y+x] ) - mean , 2 );
						nbHits++;
					}			
				}
			}			
		}
		if(nbHits==0)return new double[] {0,0};
		return new double[] { mean, Math.sqrt(accumulator/nbHits)};
	}

	public static double[] statistics1DNoBlack(double[] vals){
		double epsilon=10E-8;
		double accumulator=0;
		int hits=0;
		for(int i=0;i<vals.length ;i++) {if(vals[i]>=1) {accumulator+=vals[i];hits++;};}
		double mean=(accumulator/hits);
		accumulator=0;
		for(int i=0;i<vals.length ;i++) if(vals[i]>=1) accumulator+=Math.pow(vals[i]-mean,2);
		double std=Math.sqrt(accumulator/hits);	
		return (new double[] {mean,std});
	}

	
	public static double[] statistics1D(double[] vals){
		double accumulator=0;
		int hits=0;
		for(int i=0;i<vals.length ;i++) {accumulator+=vals[i];hits++;}
		double mean=(accumulator/hits);
		accumulator=0;
		for(int i=0;i<vals.length ;i++) accumulator+=Math.pow(vals[i]-mean,2);
		double std=Math.sqrt(accumulator/hits);	
		return (new double[] {mean,std});
	}

	
	public static double[] minAndMaxOfTab(double[] vals){
		double min=10E35;
		double max=-10E35;
		for(int i=0;i<vals.length ;i++) {
			if(vals[i]>max)max=vals[i];
			if(vals[i]<min)min=vals[i];
		}
		return (new double[] {min,max});
	}

	
	
	public static double[] statistics2D(double[][] vals){
		double accumulator=0;
		int hits=0;
		for(int i=0;i<vals.length ;i++)for(int j=0;j<vals[i].length;j++) {accumulator+=vals[i][j];hits++;}
		double mean=(accumulator/hits);
		accumulator=0;
		for(int i=0;i<vals.length ;i++)for(int j=0;j<vals[i].length;j++) accumulator+=Math.pow(vals[i][j]-mean,2);
		double std=Math.sqrt(accumulator/hits);	
		return (new double[] {mean,std});
	}

	public static ImagePlus removeCapillaryFromRandomMriImage(ImagePlus imgIn) {
		ImagePlus rec=new Duplicator().run(imgIn);	
		ImagePlus img=new Duplicator().run(imgIn);	
		rec=VitimageUtils.gaussianFiltering(rec, 0.3,0.3, 1.0);	
		double val=VitimageUtils.maxOfImage(rec);
		System.out.println("Max detecté ="+val);
		IJ.run(img,"32-bit","");
		IJ.run(img,"Divide...","value="+val+" stack");
		img.getProcessor().resetMinAndMax();
		ImagePlus ret=VitimageUtils.removeCapillaryFromHyperImageForRegistration(img);
		IJ.run(ret,"Multiply...","value="+val+" stack");
		return ret;
	}

	public static ImagePlus removeCapillaryFromHyperImageForRegistration(ImagePlus imgInit) {
		double sigmaFilter=0.3;
		ImagePlus img=new Duplicator().run(imgInit);
		ImagePlus img2=new Duplicator().run(imgInit);
		IJ.run(img2,"Multiply...","value=1000 stack");
		ImagePlus imgSliceInput;
		int xMax=img.getWidth();
		int yMax=img.getHeight();
		int zMax=img.getStackSize();
		double diamCap=0.7;
		double valThresh=200;
		double x0RoiCap;
		double y0RoiCap;
		RoiManager rm=RoiManager.getRoiManager();
		img2=VitimageUtils.gaussianFiltering(img2, sigmaFilter,sigmaFilter, sigmaFilter*3);
		img2=VitimageUtils.connexe(img2, valThresh, 10E10, 0, 10E10,6,2,true);
		IJ.run(img2,"8-bit","");
		ImageStack isRet=new ImageStack(img2.getWidth(),img.getHeight(),img.getStackSize());
		for(int z=1;z<=zMax;z++) {
			ImagePlus imgSlice=new ImagePlus("", img2.getStack().getProcessor(z));
			imgSlice.getProcessor().setMinAndMax(0,255);
			IJ.setThreshold(imgSlice, 255,255);
			for(int dil=0;dil<(diamCap/img.getCalibration().pixelWidth);dil++) IJ.run(imgSlice, "Dilate", "stack");
			//VitimageUtils.imageChecking(imgSlice,"After Dil");
			if(VitimageUtils.isNullImage(imgSlice)) {
				imgSliceInput=new ImagePlus("", img.getStack().getProcessor(z));
				if(imgSliceInput.getType()==ImagePlus.GRAY32) {
					isRet.setProcessor(imgSliceInput.getProcessor(),z);
				}
				else if(imgSliceInput.getType()==ImagePlus.GRAY16) {
					isRet.setProcessor(imgSlice.getProcessor(),z);
				}
				else if(imgSliceInput.getType()==ImagePlus.GRAY8) {
					isRet.setProcessor(imgSlice.getProcessor(),z);
				}
				else IJ.log("Remove capillary : image type not handled ("+imgSliceInput.getType()+")");	
			}
			else {
				rm.reset();
				Roi capArea=new ThresholdToSelection().convert(imgSlice.getProcessor());	
				rm.add(imgSlice, capArea, 0);							
				FloatPolygon tabPoly=capArea.getFloatPolygon();
				Rectangle rect=tabPoly.getBounds();
				int xMinRoi=(int) (rect.getX());
				int yMinRoi=(int) (rect.getY());
				int xSizeRoi=(int) (rect.getWidth());
				int ySizeRoi=(int) (rect.getHeight());
				int xMaxRoi=xMinRoi+xSizeRoi;
				int yMaxRoi=yMinRoi+ySizeRoi;				
				imgSliceInput=new ImagePlus("", img.getStack().getProcessor(z));
				if(imgSliceInput.getType()==ImagePlus.GRAY32) {
					float[] valsImg=(float[])(imgSliceInput).getProcessor().getPixels();
					//Remplacer les pixels de la zone du capillaire par des pixels copiés depuis le coin en haut à gauche de l'image 
					for(int xx=xMinRoi;xx<=xMaxRoi;xx++) for(int yy=yMinRoi;yy<yMaxRoi;yy++) if(tabPoly.contains(xx,yy)) valsImg[xMax*yy+xx]=valsImg[xMax*(yy-yMinRoi+7)+(xx-xMinRoi+7)];
					isRet.setProcessor(imgSliceInput.getProcessor(),z);
				}
				else if(imgSliceInput.getType()==ImagePlus.GRAY16) {
					short[] valsImg=(short[])(imgSliceInput).getProcessor().getPixels();
					//Remplacer les pixels de la zone du capillaire par des pixels copiés depuis le coin en haut à gauche de l'image 
					for(int xx=xMinRoi;xx<=xMaxRoi;xx++) for(int yy=yMinRoi;yy<yMaxRoi;yy++) if(tabPoly.contains(xx,yy)) valsImg[xMax*yy+xx]=valsImg[xMax*(yy-yMinRoi+7)+(xx-xMinRoi+7)];
					isRet.setProcessor(imgSlice.getProcessor(),z);
				}
				else if(imgSliceInput.getType()==ImagePlus.GRAY8) {
					byte[] valsImg=(byte[])(imgSliceInput).getProcessor().getPixels();
					//Remplacer les pixels de la zone du capillaire par des pixels copiés depuis le coin en haut à gauche de l'image 
					for(int xx=xMinRoi;xx<=xMaxRoi;xx++) for(int yy=yMinRoi;yy<yMaxRoi;yy++) if(tabPoly.contains(xx,yy)) valsImg[xMax*yy+xx]=valsImg[xMax*(yy-yMinRoi+7)+(xx-xMinRoi+7)];
					isRet.setProcessor(imgSlice.getProcessor(),z);
				}
				else IJ.log("Remove capillary : image type not handled ("+imgSliceInput.getType()+")");
			}
		}
		ImagePlus res=new ImagePlus("Result_"+img.getShortTitle()+"_no_cap.tif",isRet);
		VitimageUtils.adjustImageCalibration(res,img);
		return res;	
	}
	
	
	
	public static int[][]listForThreads(int nbP,int nbProc){
		int [][]indexes=new int[nbProc][];
		ArrayList[]arrs=new ArrayList[nbProc];
		int nbParProc=(int)Math.ceil(nbP*1.0/nbProc);
		for(int pro=0;pro<nbProc;pro++) arrs[pro]=new ArrayList<Integer>();
		for(int ind=0;ind<nbP;ind++)arrs[ind%nbProc].add(new Integer(ind));
		for(int pro=0;pro<nbProc;pro++) {indexes[pro]=new int[arrs[pro].size()]; for (int i=0;i<arrs[pro].size();i++) indexes[pro][i]=(Integer)(arrs[pro].get(i));  }
		return indexes;
/*		int nThread=nbProc-1;
		int[]tabSendToThread=new int[Z];
		int[]placeIntoThread=new int[Z];
		int[]incrPlacesThread=new int[nThread];
		int[][]tabSlicesOfEachThread=new int[nThread][];
		for(int z=0;z<Z;z++) {
			tabSendToThread[z]=z%nThread;
			placeIntoThread[z]=incrPlacesThread[z%nThread]++;
		}
		for(int nt=0;nt<nThread;nt++) {
			tabSlicesOfEachThread[nt]=new int[incrPlacesThread[nt]];
			incrPlacesThread[nt]=0;
		}
		for(int z=0;z<Z;z++) {
			placeIntoThread[z]=incrPlacesThread[z%nThread];
			tabSlicesOfEachThread[z%nThread][incrPlacesThread[z%nThread]++]=z;
		}
*/
	}
	
	
	public static double[]getVoxelSizes(ImagePlus img){
		return new double[] {img.getCalibration().pixelWidth,img.getCalibration().pixelHeight,img.getCalibration().pixelDepth};
	}
	
	public static int[]getDimensions(ImagePlus img){
		return new int[] {img.getWidth(),img.getHeight(),img.getStackSize()};
	}

	public static double[]getDimensionsRealSpace(ImagePlus img){
		int[]dims=getDimensions(img);
		double[]voxs=getVoxelSizes(img);
		double[]ret=new double[] {dims[0]*voxs[0],dims[1]*voxs[1],dims[2]*voxs[2]};
		return ret;
	}

	public static int[]getDimensionsXYZCT(ImagePlus img){
		int[]tab1=img.getDimensions();
		return new int[] {tab1[0],tab1[1],tab1[3],tab1[2],tab1[4]};
	}
	
	public static double dou(double d){
		if(d<0)return (-dou(-d));
		if (d<0.0001)return 0;
		return (double)(Math.round(d * 10000)/10000.0);
	}
	public static double dou(double d,int n){
		if(d<0)return (-dou(-d));
		if (d<Math.pow(10, -n))return 0;
		return (double)(Math.round(d * Math.pow(10, n))/Math.pow(10, n));
	}
	
	// TODO : Throw away the three following, keeping the last one
	public static ImagePlus[]stacksFromHyperstack(ImagePlus hyper,int nb){
		ImagePlus []ret=new ImagePlus[nb];
		for(int i=0;i<nb;i++) {
			IJ.run(hyper,"Make Substack...","slices=1-"+(hyper.getStackSize()/nb)+" frames="+(i+1)+"-"+(i+1)+"");
			ret[i]=IJ.getImage();
			ret[i].setTitle("Splitting hyperstack, frame "+i);
			ret[i].hide();
			VitimageUtils.adjustImageCalibration(ret[i],hyper);
		}
		return ret;
	}

	
	public static ImagePlus[]stacksFromHyperstackBis(ImagePlus hyper){
		int nbZ=hyper.getNSlices();
		int nbT=hyper.getNFrames();
		int nbC=hyper.getNChannels();
		int nb=nbT*nbC;
		ImagePlus []ret=new ImagePlus[nb];
		for(int ic=0;ic<nbC;ic++) {
			for(int it=0;it<nbT;it++) {
				System.out.println(ic+"/"+nbC+" ,  "+it+"/"+nbT);
				int i=ic*nbT+it;
				if(nbC>1 && nbT==1) 						IJ.run(hyper,"Make Substack...","channels="+(ic+1)+"-"+(ic+1)+" slices=1-"+nbZ+"");
				else if(nbC==1 && nbT>1) 					IJ.run(hyper,"Make Substack...","slices=1-"+nbZ+" frames="+(it+1)+"-"+(it+1)+"");
				else if(nbC>1 && nbT>1) 					IJ.run(hyper,"Make Substack...","channels="+(ic+1)+"-"+(ic+1)+" slices=1-"+nbZ+" frames="+(it+1)+"-"+(it+1)+"");
				else 										IJ.run(hyper,"Make Substack...","slices=1-"+nbZ+"");
				ret[i]=IJ.getImage();
				System.out.println("Attrape la bonne : "+TransformUtils.stringVectorN(ret[i].getDimensions(),""));
				System.out.println("Dont le titre est = :"+ret[i].getTitle());
				ret[i].setTitle("Splitting hyperstack, channel "+ic+" frame "+it);
				ret[i].hide();
				VitimageUtils.adjustImageCalibration(ret[i],hyper);
			}
		}
		return ret;
	}
	

	
	
	public static ImagePlus[]stacksFromHyperstackFast(ImagePlus hyper,int nb){
		ImagePlus []ret=new ImagePlus[nb];
		int sli=hyper.getStackSize()/nb;
		System.out.println("En effet : nbtotal slices="+(sli*9));
		for(int i=0;i<nb;i++) {
			ret[i] = new Duplicator().run(hyper, 1, 1, 1, sli, (i+1), (i+1));
			VitimageUtils.adjustImageCalibration(ret[i],hyper);
		}
		return ret;
	}
	
	

	public static ImagePlus[]stacksFromHyperstackFastBis(ImagePlus hyper){
		int nbZ=hyper.getNSlices();
		int nbT=hyper.getNFrames();
		int nbC=hyper.getNChannels();
		int nb=nbT*nbC;
		ImagePlus []ret=new ImagePlus[nb];
		for(int ic=0;ic<nbC;ic++) {
			for(int it=0;it<nbT;it++) {
				int i=ic*nbT+it;
				System.out.println(ic+"/"+nbC+" ,  "+it+"/"+nbT);
				ret[i] = new Duplicator().run(hyper, 1+ic, 1+ic, 1, nbZ, 1+it, 1+it);
				VitimageUtils.adjustImageCalibration(ret[i],hyper);
				IJ.run(ret[i],"Grays","");
			}
		}
		return ret;
	}
	
	
	
	
	
	
	public static void imageChecking(ImagePlus imgInit,double sliMin,double sliMax,int periods,String message,double totalDuration,boolean fluidVisu) {
		System.out.println("Verifying image names "+message+" with dimensions= "+TransformUtils.stringVector(VitimageUtils.getDimensions(imgInit), "")+" with voxel sizes="+TransformUtils.stringVector(VitimageUtils.getVoxelSizes(imgInit), ""));
		int minFrameRateForVisualConfort=33;
		int maxDurationForVisualConfort=1000/minFrameRateForVisualConfort;
		if (imgInit==null)return;
		ImagePlus img=new Duplicator().run(imgInit,1,imgInit.getStackSize());
		img.getProcessor().resetMinAndMax();
		String titleOld=img.getTitle();
		String str;
		if (message.compareTo("")==0)str=titleOld;
		else str=message;
		img.setTitle(str);
		int sliceMin=(int)Math.round(sliMin);
		int sliceMax=(int)Math.round(sliMax);
		int miniDuration=0;
		if(periods<1)periods=1;
		if(sliceMin<1)sliceMin=1;
		if(sliceMin>img.getStackSize())sliceMin=img.getStackSize();
		if(sliceMax<1)sliceMax=1;
		if(sliceMax>img.getStackSize())sliceMax=img.getStackSize();
		if(sliceMin>sliceMax)sliceMin=sliceMax;
		img.show();
		if(img.getType() != ImagePlus.COLOR_RGB)IJ.run(img,"Fire","");
		if(sliceMin==sliceMax) {
			miniDuration=(int)Math.round(1000.0*totalDuration/periods);
			img.setSlice(sliceMin);
			for(int i=0;i<periods;i++)
				waitFor(miniDuration);
			return;
		}
		else {
			miniDuration=(int)Math.round(totalDuration*1000.0/periods/(sliceMax-sliceMin+10));
			while(fluidVisu && miniDuration>maxDurationForVisualConfort) {
				periods++;
				miniDuration=(int)Math.round(totalDuration*1000.0/periods/(sliceMax-sliceMin+10));				
			}
			int curSlice=(sliceMin+sliceMax)/2;
			for(int j=0;j<5 ;j++)waitFor(miniDuration);
			img.setSlice((sliceMin+sliceMax/2));
			for(int i=0;i<periods;i++) {
				while (curSlice>sliceMin) {
					img.setSlice(--curSlice);
					waitFor(miniDuration);
				}
				for(int j=0;j<(sliMax-sliMin)/8 ;j++)waitFor(miniDuration);
				while (curSlice<sliceMax) {
					img.setSlice(++curSlice);
					waitFor(miniDuration);
				}
				for(int j=0;j<5 ;j++)waitFor(miniDuration);
			}
		}
		img.close();
	}
	public static void imageChecking(ImagePlus img,String message,double totalDuration) {
		if (message.compareTo("")==0)imageChecking(img,0,img.getStackSize()-1,1,img.getTitle(),totalDuration,true);
		else imageChecking(img,0,img.getStackSize()-1,1,message,totalDuration,true);
	}
	public static void imageCheckingFast(ImagePlus img,String message) {
		if (message.compareTo("")==0)imageChecking(img,0,img.getStackSize()-1,1,img.getTitle(),2,true);
		else imageChecking(img,0,img.getStackSize()-1,1,message,2,true);
	}

	public static void imageChecking(ImagePlus img,String message) {
		if (message.compareTo("")==0)imageChecking(img,0,img.getStackSize()-1,1,img.getTitle(),4,true);
		else imageChecking(img,0,img.getStackSize()-1,1,message,4,true);
	}
	public static void imageChecking(ImagePlus img,double totalDuration) {
		imageChecking(img,0,img.getStackSize()-1,1,img.getTitle(),totalDuration,true);
	}
	public static void imageChecking(ImagePlus img) {
		imageChecking(img,0,img.getStackSize()-1,1,img.getTitle(),3,true);
	}
	
	public static void waitFor(int n) {
		try {
			java.util.concurrent.TimeUnit.MILLISECONDS.sleep(n);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}		
	}
	
	public static ImagePlus gaussianFilteringIJ(ImagePlus imgIn,double sigmaX,double sigmaY,double sigmaZ) {
		ImagePlus img=new Duplicator().run(imgIn);
		double []voxSizes=VitimageUtils.getVoxelSizes(imgIn);
		double sigX=sigmaX/voxSizes[0];
		double sigY=sigmaY/voxSizes[1];
		double sigZ=sigmaZ/voxSizes[2];
		IJ.run(img, "Gaussian Blur 3D...", "x="+sigX+" y="+sigY+" z="+sigZ);		
		return img;
	}
	
	
	public static ImagePlus gaussianFiltering(ImagePlus imgIn,double sigmaX,double sigmaY,double sigmaZ) {
		Image img=ItkImagePlusInterface.imagePlusToItkImage(imgIn);
		
		/*
		DiscreteGaussianImageFilter gaussFilter=new DiscreteGaussianImageFilter();
		VectorDouble var=new VectorDouble(3);
		var.set(0,sigmaX*sigmaX);
		var.set(1,sigmaY*sigmaY);
		var.set(2,sigmaZ*sigmaZ);
		VectorDouble err=new VectorDouble(3);
		err.set(0,0.01);
		err.set(1,0.01);
		err.set(2,0.01);
		img=gaussFilter.execute(img,var,50, err,true);
		*/
	
		RecursiveGaussianImageFilter gaussFilter=new RecursiveGaussianImageFilter();
		if(imgIn.getWidth()>=4 && sigmaX>0) {
			gaussFilter.setDirection(0);
			gaussFilter.setSigma(sigmaX);
			img=gaussFilter.execute(img);
		}
		else System.out.println("Gaussian filterin : no work with X");
		if(imgIn.getHeight()>=4 && sigmaY>0) {
			gaussFilter.setDirection(1);
			gaussFilter.setSigma(sigmaY);
			img=gaussFilter.execute(img);
		}
		else System.out.println("Gaussian filterin : no work with Y");
		if(imgIn.getStackSize()>=4 && sigmaZ>0) {
			gaussFilter.setDirection(2);
			gaussFilter.setSigma(sigmaZ);
			img=gaussFilter.execute(img);
		}
		else System.out.println("Gaussian filterin : no work with Z");
		
		return ItkImagePlusInterface.itkImageToImagePlus(img);
	}
	
	public static ImagePlus getTestImage(String title) {
		System.out.println("Ouverture d image test : "+System.getProperty("user.dir")+slash+"src/test/imgs"+slash+title);
		return IJ.openImage(System.getProperty("user.dir")+slash+"src/test/imgs"+slash+title);
	}
	
	public static void saveTestResult(ImagePlus img, String title) {
		System.out.println("Sauvegarde image resultat : "+System.getProperty("user.dir")+slash+"src/test/imgs"+slash+title);
		IJ.saveAsTiff(img, System.getProperty("user.dir")+slash+"src/test/imgs"+slash+title);
	}
	
	
	/** Create a Thread[] array as large as the number of processors available. 
	 * From Stephan Preibisch's Multithreading.java class. See: 
	 * http://repo.or.cz/w/trakem2.git?a=blob;f=mpi/fruitfly/general/MultiThreading.java;hb=HEAD 
	 */  
	public static Thread[] newThreadArray(int n) {  
		int n_cpus = Runtime.getRuntime().availableProcessors();  
		return new Thread[n];  
	}  

	/** Start all given threads and wait on each of them until all are done. 
	 * From Stephan Preibisch's Multithreading.java class. See: 
	 * http://repo.or.cz/w/trakem2.git?a=blob;f=mpi/fruitfly/general/MultiThreading.java;hb=HEAD 
	 */  
	public static void startAndJoin(Thread[] threads){  
		for (int ithread = 0; ithread < threads.length; ++ithread){  
			threads[ithread].setPriority(Thread.NORM_PRIORITY);  
			threads[ithread].start();  
		}
		try{     
			for (int ithread = 0; ithread < threads.length; ++ithread)  
				threads[ithread].join();  
		} catch (InterruptedException ie) {  	System.out.println(ie.getStackTrace());throw new RuntimeException(ie);  }  
	}   

	public static void startAndJoin(Thread thread){  
		thread.setPriority(Thread.NORM_PRIORITY);  
		thread.start();  
		try{     
			thread.join();  
		} catch (InterruptedException ie) {  	System.out.println(ie.getStackTrace());throw new RuntimeException(ie);  }  
	}   

	
	public static String[] stringArraySort(String[]tabStr) {
		String[]tabRet=new String[tabStr.length];
		ArrayList<String> listStr=new ArrayList<String>();
		for(String str : tabStr)listStr.add(str);
		Collections.sort(listStr);
		for(int i=0;i<listStr.size();i++)tabRet[i]=listStr.get(i);
		return tabRet;
	}


	
	
	
		
	public static ImagePlus makeWekaSegmentation(ImagePlus imgToSegment,String pathToClassifier) {
		WekaSegmentation weka= new WekaSegmentation(imgToSegment);
		weka.loadClassifier(pathToClassifier);
		weka.loadNewImage(imgToSegment);
//		weka.fea
		weka.applyClassifier(false);
		ImagePlus res=weka.getClassifiedImage();
		VitimageUtils.adjustImageCalibration(res,imgToSegment);
		return res;
	}

	
	public static Point3d coordinatesOfObjectInSlice(ImagePlus img,int slice,boolean realCoords){
		double []voxSize=VitimageUtils.getVoxelSizes(img);
		if(img.getType() != ImagePlus.GRAY8) System.out.println("Erreur in coordinatesOfObjectInSlice: image "+img.getTitle()+" should be 8 bit");
		if(slice>=img.getStackSize() || slice<0)System.out.println("Erreur in coordinatesOfObjectInSlice: image "+img.getTitle()+" was asked for slice number "+slice);
		int xM=img.getWidth();
		int yM=img.getHeight();
		int nbHits=0;
		double coords[]=new double[3];
		byte[] valsImg=(byte [])img.getStack().getProcessor(slice+1).getPixels();
		for(int x=0;x<xM;x++)for(int y=0;y<yM;y++)if (  (int)(  (  (byte)valsImg[xM*y+x])  & 0xff)  == 255) {
			nbHits++;
			coords[0]+=x;
			coords[1]+=y;
			coords[2]+=slice;
		}			
		for(int dim=0;dim<3;dim++) {
			coords[dim]/=nbHits;
			if(realCoords)coords[dim]*=voxSize[dim];
		}
		return new Point3d(coords[0],coords[1],coords[2]);
	}
	public static Point3d coordinatesOfObject(ImagePlus img,boolean realCoords){
		double []voxSize=VitimageUtils.getVoxelSizes(img);
		if(img.getType() != ImagePlus.GRAY8) System.out.println("Erreur in coordinatesOfObjectInSlice: image "+img.getTitle()+" should be 8 bit");
		int xM=img.getWidth();
		int yM=img.getHeight();
		int zM=img.getStackSize();
		int nbHits=0;
		double coords[]=new double[3];
		for(int z=0;z<zM;z++) {
			byte[] valsImg=(byte [])img.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<xM;x++)for(int y=0;y<yM;y++)if (  (int)(  (  (byte)valsImg[xM*y+x])  & 0xff)  == 255) {
				nbHits++;
				coords[0]+=x;
				coords[1]+=y;
				coords[2]+=z;
			}		
		}
		for(int dim=0;dim<3;dim++) {
			coords[dim]/=nbHits;
			if(realCoords)coords[dim]*=voxSize[dim];
		}
		return new Point3d(coords[0],coords[1],coords[2]);
	}

	public static int[]getRadiusInVoxels(double[]voxSizes,double radius,boolean is3D){
		int[]valsRet=new int[3];
		for(int i=0;i<3;i++) valsRet[i]=(int)Math.round(radius/voxSizes[i]);
		if(!is3D)valsRet[2]=0;
		return valsRet;
	}
	
	
	public static ImagePlus buildImageLookup(double[][][]tabDist,double[]voxSizes,double radius) {
		int dimX=tabDist.length;
		int dimY=tabDist[0].length;
		int dimZ=tabDist[0][0].length;
		ImagePlus imgDist = IJ.createImage("lookup_mask", "32-bit black",dimX,dimY,dimZ);
		VitimageUtils.adjustImageCalibration(imgDist, voxSizes,"mm");
		for(int x=0;x<dimX;x++) {
			for(int y=0;y<dimY;y++) {
				for(int z=0;z<dimZ;z++) {
					(imgDist.getStack().getProcessor(z+1)).putPixelValue(x,y,(tabDist[x][y][z]<=radius ? tabDist[x][y][z] : 0));
				}
			}
		}
		IJ.run(imgDist,"Fire","");
		imgDist.setDisplayRange(0, radius);
		imgDist.setSlice(imgDist.getStackSize()/2+1);
		return imgDist;
	}
	
	
	public static ImagePlus buildFullSegFromParts() {
		ImagePlus segs[]=new ImagePlus[5];
		segs[0]=IJ.openImage("/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/CEP011_AS1/segmentation_BG.tif");
		segs[1]=IJ.openImage("/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/CEP011_AS1/segmentation_SAIN.tif");
		segs[2]=IJ.openImage("/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/CEP011_AS1/segmentation_NECROSE.tif");
		segs[3]=IJ.openImage("/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/CEP011_AS1/segmentation_AMADOU.tif");
		segs[4]=IJ.openImage("/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/CEP011_AS1/segmentation_ECORCE.tif");
		ImagePlus segSum=VitimageUtils.imageCopy(segs[0]);
		IJ.run(segSum,"32-bit","");
		segSum=VitimageUtils.makeOperationOnOneImage(segSum, 2, 0, false);
		for(int s=0;s<5;s++) {
			IJ.run(segs[s],"32-bit","");
			segs[s]=VitimageUtils.makeOperationOnOneImage(segs[s],2,s, true);
			segSum=VitimageUtils.makeOperationBetweenTwoImages(segSum,segs[s],1,true);
		}
		segSum=VitimageUtils.makeOperationOnOneImage(segSum, 3,255,true);
		segSum.setDisplayRange(0,255);
		IJ.run(segSum,"8-bit","");
		IJ.run(segSum,"Fire","");
		segSum.setDisplayRange(0,5);
		segSum.show();
		return segSum;
	}

	
	
	public static ImagePlus mostRepresentedFilteringWithRadius(ImagePlus imgInTmp,double radius,boolean is3D,int maxNbClasses,boolean doPadding) {
		double []voxS=VitimageUtils.getVoxelSizes(imgInTmp);
		int[]radiusVox=getRadiusInVoxels(voxS,radius,is3D);
		int rayX=radiusVox[0];
		int rayY=radiusVox[1];
		int rayZ=radiusVox[2];
	
		boolean[][][]lookup=buildLookup(radius,radiusVox,voxS);
		double[][][]distances=buildDistances(radiusVox,voxS);
		ImagePlus imgLookup=buildImageLookup(distances,voxS,radius);
		imgLookup.show();
		ImagePlus imgIn=new Duplicator().run(imgInTmp);
		VitimageUtils.adjustImageCalibration(imgIn, imgInTmp);
		int dimX=imgIn.getWidth();
		int dimY=imgIn.getHeight();
		int dimZ=imgIn.getStackSize();
		System.out.println("Effectivement dims avant="+dimX+","+dimY+","+dimZ);
		if(doPadding) {
			imgIn= uncropImageByte(imgIn,rayX,rayY,rayZ,dimX+2*rayX,dimY+2*rayY,dimZ+2*rayZ);
			imgIn.show();
			imgIn.setSlice(rayZ+1);
			IJ.run(imgIn, "Select All", "");
			IJ.run(imgIn, "Copy", "");
			for(int z=0;z<rayZ;z++) {
				imgIn.setSlice(z+1);
				IJ.run(imgIn, "Paste", "");
			}
			imgIn.setSlice(dimZ+2*rayX-rayZ);
			IJ.run(imgIn, "Select All", "");
			IJ.run(imgIn, "Copy", "");
			for(int z=0;z<rayZ;z++) {
				imgIn.setSlice(dimZ+2*rayX-rayZ+z+1);
				IJ.run(imgIn, "Paste", "");
			}
			imgIn.hide();		
			dimX=imgIn.getWidth();
			dimY=imgIn.getHeight();
			dimZ=imgIn.getStackSize();
			System.out.println("Effectivement dims apres="+dimX+","+dimY+","+dimZ);
		}
		ImagePlus imgOut=imgIn.duplicate();
		VitimageUtils.adjustImageCalibration(imgOut, imgIn);
		int []vals=new int[lookup.length*lookup[0].length*lookup[0][0].length];
		double[] dist=new double[lookup.length*lookup[0].length*lookup[0][0].length];
		byte[][] valsImg=new byte[dimZ][];
		byte[][] valsOut=new byte[dimZ][];
		for(int z=0;z<dimZ;z++) {
			valsImg[z]=(byte[])imgIn.getStack().getProcessor(z+1).getPixels();
			valsOut[z]=(byte[])imgOut.getStack().getProcessor(z+1).getPixels();
		}			
		for(int z=rayZ;z<dimZ-rayZ;z++) {
			if(z%10==0)System.out.print("  "+z+"/"+dimZ);
			if(z%100==0)System.out.println(z+"/"+dimZ);
			for(int x=rayX;x<dimX-rayX;x++) {
				for(int y=rayY;y<dimY-rayY;y++){
					int index=0;
					for(int dz=-rayZ;dz<rayZ+1;dz++) {
						for(int dx=-rayX;dx<rayX+1;dx++) {
							for(int dy=-rayY;dy<rayY+1;dy++){
								if(! lookup[dx+rayX][dy+rayY][dz+rayZ]) {
									dist[index]=10E8;
									vals[index++]=-1;
								}
								else {
									vals[index]=((byte)(valsImg[z+dz][dimX*(y+dy)+(x+dx)]) & 0xff);
									dist[index++]=distances[dx+rayX][dy+rayY][dz+rayZ];
								}
							}
						}
					}
					valsOut[z][dimX*(y)+(x)]=((byte)(mostRepresentedValue(vals, dist,maxNbClasses)  & 0xff));
				}			
			}
		}
		imgLookup.close();
		if(doPadding)imgOut=VitimageUtils.cropImageByte(imgOut, rayX, rayY, rayZ,dimX-2*rayX, dimY-2*rayY, dimZ-2*rayZ);
		IJ.run(imgOut,"Fire","");
		imgOut.resetDisplayRange();
		return imgOut;
	}
	

	public static double[][][]buildDistances(int []radiusVox,double[]voxSizes){
		int rayX=radiusVox[0];
		int rayY=radiusVox[1];
		int rayZ=radiusVox[2];
		double[][][]tab=new double[2*rayX+1][2*rayY+1][2*rayZ+1];
		int I=tab.length;
		int J=tab[0].length;
		int K=tab[0][0].length;			
		for(int i=0;i<I;i++) {
			for(int j=0;j<J;j++) {
				for(int k=0;k<K;k++) {
				tab[i][j][k]=(Math.sqrt( (rayX-i) * (rayX-i)*voxSizes[0]*voxSizes[0] +
										 (rayY-j) * (rayY-j)*voxSizes[1]*voxSizes[1] +
										 (rayZ-k) * (rayZ-k)*voxSizes[2]*voxSizes[2]));
				}
			}
		}
		return tab;
	}

	
	public static boolean[][][]buildLookup(double radius,int []radiusVox,double[]voxSizes){
		int rayX=radiusVox[0];
		int rayY=radiusVox[1];
		int rayZ=radiusVox[2];
		boolean[][][]tab=new boolean[2*rayX+1][2*rayY+1][2*rayZ+1];
		int I=tab.length;
		int J=tab[0].length;
		int K=tab[0][0].length;
		for(int i=0;i<I;i++) {
			for(int j=0;j<J;j++) {
				for(int k=0;k<K;k++) {
					tab[i][j][k]=(Math.sqrt( (rayX-i) * (rayX-i)*voxSizes[0]*voxSizes[0] +
							 (rayY-j) * (rayY-j)*voxSizes[1]*voxSizes[1] +
							 (rayZ-k) * (rayZ-k)*voxSizes[2]*voxSizes[2]) < radius);
				}
			}
		}
		return tab;
	}
	
	//"CEP011_AS1" "CEP012_AS2" "CEP013_AS3" "CEP014_RES1" "CEP015_RES2" "CEP016_RES3" "CEP017_S1" "CEP018_S2" "CEP019_S3" "CEP020_APO1" "CEP021_APO2" "CEP022_APO3"

	public static String[]getSpecimenNames(){
		return	new String[] {"CEP011_AS1","CEP012_AS2","CEP013_AS3","CEP014_RES1","CEP015_RES2","CEP016_RES3","CEP017_S1","CEP018_S2","CEP019_S3","CEP020_APO1","CEP021_APO2","CEP022_APO3"};
	}

	
	public static int mostRepresentedValue(int []vals, double []distances,int MAX_NB_CLASSES) {
		double HIGH_DISTANCE=10E10;
		int[]hits=new int[MAX_NB_CLASSES];
		double[]distMin=new double[MAX_NB_CLASSES];
		for(int i=0;i<distMin.length;i++)distMin[i]=HIGH_DISTANCE;

		for(int i=0;i<vals.length;i++) {
			if(vals[i]<0)continue;
			hits[vals[i]]++;
			if(distances[i]<distMin[vals[i]])distMin[vals[i]]=distances[i];
		}
		
		
		
		//recherche du maximum represente le moins distant
		int valMax=0;
		int indMax=-1;
		double distMinOfMax=HIGH_DISTANCE;
		for(int i= 0;i<MAX_NB_CLASSES;i++) {
			if( (hits[i]>valMax)) {
				valMax=hits[i];
				indMax=i;
				distMinOfMax=distMin[i];
			}
			else if( (hits[i]==valMax) && (distMin[i]<distMinOfMax)) {
				valMax=hits[i];
				indMax=i;
				distMinOfMax=distMin[i];
			}
		}
		return indMax;
	}
	

	
	
	
	public static ImagePlus maskRGB(ImagePlus imgRGB,ImagePlus imgMaskTmp) {
		ImagePlus imgMask=new Duplicator().run(imgMaskTmp);
		VitimageUtils.adjustImageCalibration(imgMask, imgMaskTmp);
		IJ.run(imgMask, "Divide...", "value=255 stack");
		
		ImagePlus[] channels = ChannelSplitter.split(imgRGB);
		for(int i=0;i<3;i++)channels[i]=new ImageCalculator().run("Multiply create stack", channels[i], imgMask);
		ImagePlus ret=new ImagePlus("",RGBStackMerge.mergeStacks(channels[0].getStack(),channels[1].getStack(),channels[2].getStack(),true));
		VitimageUtils.adjustImageCalibration(ret, imgRGB);
		return ret;
	}
	

	
	public static ImagePlus normalizationSliceRGBHSV(ImagePlus img,int rayX,int rayY,int rayZup,int rayZdown,boolean statsExcludingBlackPoints,boolean excludeHue) {
		ImagePlus imgIn=new Duplicator().run(img);
		VitimageUtils.adjustImageCalibration(imgIn, img);
		IJ.run(imgIn, "HSB Stack", "");
		ImagePlus[] channels = ChannelSplitter.split(imgIn);
		for(int i=(excludeHue ? 1 : 0);i<3;i++) {
			//channels[i].show();
			//VitimageUtils.waitFor(5000);
			channels[i]=normalizationSliceByte2(channels[i],rayX,rayZup,rayZdown, statsExcludingBlackPoints);
//			channels[i]=normalizationSliceByte(channels[i],rayX,rayY,rayZup,rayZdown,statsExcludingBlackPoints);
			//channels[i].show();
			//VitimageUtils.waitFor(5000);
		}
		
		ImagePlus ret=new ImagePlus("",RGBStackMerge.mergeStacks(channels[0].getStack(),channels[1].getStack(),channels[2].getStack(),true));
		IJ.run(ret,"Color Space Converter", "from=HSB to=RGB white=D65"); 
		VitimageUtils.adjustImageCalibration(ret, img);
		return ret;
	
	}

	
	
	public static ImagePlus normalizationSliceRGB(ImagePlus img,int rayX,int rayY,int rayZup,int rayZdown,boolean statsExcludingBlackPoints) {
		ImagePlus imgIn=new Duplicator().run(img);
		VitimageUtils.adjustImageCalibration(imgIn, img);
		ImagePlus[] channels = ChannelSplitter.split(imgIn);
		for(int i=0;i<3;i++) {
			System.out.println("Traitement channel "+i);
			//channels[i].show();
			//VitimageUtils.waitFor(5000);
			channels[i]=normalizationSliceByte3(channels[i],rayX,rayZup,rayZdown, statsExcludingBlackPoints);
//			channels[i]=normalizationSliceByte(channels[i],rayX,rayY,rayZup,rayZdown,statsExcludingBlackPoints);
			//channels[i].show();
			//VitimageUtils.waitFor(5000);
		}
		ImagePlus ret=new ImagePlus("",RGBStackMerge.mergeStacks(channels[0].getStack(),channels[1].getStack(),channels[2].getStack(),true));
		VitimageUtils.adjustImageCalibration(ret, img);
		return ret;
	
	}
	
	
	public static ImagePlus[] meanAndVarOnlyValidValuesByte(ImagePlus img,int rayX) {
		ImagePlus imgMean=new Duplicator().run(img);
		VitimageUtils.adjustImageCalibration(imgMean, img);
		IJ.run(imgMean,"32-bit","");
		ImagePlus imgVar=new Duplicator().run(img);
		VitimageUtils.adjustImageCalibration(imgVar, img);
		IJ.run(imgVar,"32-bit","");
		ImagePlus imgMask=VitimageUtils.thresholdByteImage(img, 0,1);
		RoiManager rm=RoiManager.getRoiManager();
		int dimZ=img.getStackSize();
		System.out.print("Moyenne et variance dans masque ...");
		for(int z=1;z<=dimZ;z++) {
			if(z%10==0)System.out.print("   "+z+"/"+dimZ);
			imgMean.setSlice(z);
			imgVar.setSlice(z);
			imgMask.setSlice(z);
			IJ.run(imgMask, "Create Selection", "");			
			imgMean.setRoi(imgMask.getRoi());
			IJ.run(imgMean, "Mean...", "radius="+rayX+" slice");
			imgVar.setRoi(imgMask.getRoi());
			IJ.run(imgVar, "Variance...", "radius="+rayX+" slice");
			imgMean.deleteRoi();
			imgVar.deleteRoi();
			imgMask.deleteRoi();
		}
		System.out.println("");
		return new ImagePlus[] {imgMean,imgVar};
	}
	
	
	
	
	public static ImagePlus normalizationSliceByte3(ImagePlus img,int rayX,int rayZup,int rayZdown,boolean  statsExcludingBlackPoints) {
		double []voxS=VitimageUtils.getVoxelSizes(img);
		int dimX=img.getWidth();
		int dimY=img.getHeight();
		int dimZ=img.getStackSize();
		int zMean=(rayZup+rayZdown+1)/2;
		ImagePlus imgMean,imgVar;
		ImagePlus imgOut=new Duplicator().run(img);
		VitimageUtils.adjustImageCalibration(imgOut, img);

		if(!statsExcludingBlackPoints) {
			imgMean=new Duplicator().run(img);
			VitimageUtils.adjustImageCalibration(imgMean, img);
			imgVar=new Duplicator().run(img);
			VitimageUtils.adjustImageCalibration(imgVar, img);
			IJ.run(imgMean, "Mean...", "radius="+rayX+" stack");
			IJ.run(imgVar, "Variance...", "radius="+rayX+" stack");
			IJ.run(imgMean,"32-bit","");
			IJ.run(imgVar,"32-bit","");
		}
		else {
			ImagePlus[] tabImg =meanAndVarOnlyValidValuesByte(img,rayX);
			imgMean=tabImg[0];
			imgVar=tabImg[1];
		}
		
		byte[][] valsOut=new byte[dimZ][];
		float[][] valsMean=new float[dimZ][];
		float[][] valsVar=new float[dimZ][];
		double val,valVers,valRect,valMean2d,valMean3d,valVar2d,valVar3d,valMean2dVers,valVar2dVers,valMean2dRect,valVar2dRect;
		int valInt;
		double nSig2d;
		boolean debug;
		int nSamples=rayZup+rayZdown+1;
		double ratio=1.0/nSamples;
		for(int z=0;z<dimZ;z++) {
			valsMean[z]=(float[])imgMean.getStack().getProcessor(z+1).getPixels();
			valsVar[z]=(float[])imgVar.getStack().getProcessor(z+1).getPixels();
		//	valsVar3d[z]=(byte[])imgVar3d.getStack().getProcessor(z+1).getPixels();
			valsOut[z]=(byte[])imgOut.getStack().getProcessor(z+1).getPixels();
		}			
		for(int z=1;z<dimZ/2;z++) {
			if(z%10==0)System.out.print("   "+z+"/"+(dimZ/2));
			for(int x=rayX;x<dimX-rayX;x++) {
				for(int y=rayX;y<dimY-rayX;y++){
					valVers=(double)((int)((byte)(valsOut[z*2-1][dimX*(y)+(x)]) & 0xff));
					valRect=(double)((int)((byte)(valsOut[z*2][dimX*(y)+(x)]) & 0xff));

					valMean2dVers=(double)(valsMean[z*2-1][dimX*(y)+(x)]);
					valVar2dVers=(double)(valsVar[z*2-1][dimX*(y)+(x)]);
					valMean2dRect=(double)(valsMean[z*2][dimX*(y)+(x)]);
					valVar2dRect=(double)(valsVar[z*2][dimX*(y)+(x)]);
					valVar3d=0.5*(valVar2dVers+valVar2dRect);
					valMean3d=0.5*(valMean2dVers+valMean2dRect);

					if(valVar2dRect<1)nSig2d=0;
					else nSig2d=(valRect-valMean2dRect)/valVar2dRect;
					valInt=(int)Math.round(Math.max(Math.min( 255,valMean3d+valVar3d*nSig2d), 0));
					valsOut[z*2][dimX*(y)+(x)]=((byte)((   valInt  ) & 0xff));
					
					if(valVar2dVers<1)nSig2d=0;
					else nSig2d=(valVers-valMean2dVers)/valVar2dVers;
					valInt=(int)Math.round(Math.max(Math.min( 255,valMean3d+valVar3d*nSig2d), 0));
					valsOut[z*2-1][dimX*(y)+(x)]=((byte)((   valInt  ) & 0xff));
				}			
			}
		}
		return imgOut;
	}
		
	
	
	public static ImagePlus normalizationSliceByte2(ImagePlus img,int rayX,int rayZup,int rayZdown,boolean  statsExcludingBlackPoints) {
		double []voxS=VitimageUtils.getVoxelSizes(img);
		int dimX=img.getWidth();
		int dimY=img.getHeight();
		int dimZ=img.getStackSize();
		int zMean=(rayZup+rayZdown+1)/2;
		ImagePlus imgMean,imgVar;
		ImagePlus imgOut=new Duplicator().run(img);
		VitimageUtils.adjustImageCalibration(imgOut, img);

		if(!statsExcludingBlackPoints) {
			imgMean=new Duplicator().run(img);
			VitimageUtils.adjustImageCalibration(imgMean, img);
			imgVar=new Duplicator().run(img);
			VitimageUtils.adjustImageCalibration(imgVar, img);
			IJ.run(imgMean, "Mean...", "radius="+rayX+" stack");
			IJ.run(imgVar, "Variance...", "radius="+rayX+" stack");
			IJ.run(imgMean,"32-bit","");
			IJ.run(imgVar,"32-bit","");
		}
		else {
			ImagePlus[] tabImg =meanAndVarOnlyValidValuesByte(img,rayX);
			imgMean=tabImg[0];
			imgVar=tabImg[1];
		}
		
		byte[][] valsOut=new byte[dimZ][];
		float[][] valsMean=new float[dimZ][];
		float[][] valsVar=new float[dimZ][];
		double val,valMean2d,valMean3d,valVar2d,valVar3d;
		int valInt;
		double nSig2d;
		boolean debug;
		int nSamples=rayZup+rayZdown+1;
		double ratio=1.0/nSamples;
		for(int z=0;z<dimZ;z++) {
			valsMean[z]=(float[])imgMean.getStack().getProcessor(z+1).getPixels();
			valsVar[z]=(float[])imgVar.getStack().getProcessor(z+1).getPixels();
		//	valsVar3d[z]=(byte[])imgVar3d.getStack().getProcessor(z+1).getPixels();
			valsOut[z]=(byte[])imgOut.getStack().getProcessor(z+1).getPixels();
		}			
		for(int z=rayZup;z<dimZ-rayZdown;z++) {
			if(z%10==0)System.out.print("   "+z+"/"+dimZ);
			for(int x=rayX;x<dimX-rayX;x++) {
				for(int y=rayX;y<dimY-rayX;y++){
					debug=false;
					//if(z==74 && y==126 && x==264)debug =true;
					val=(double)((int)((byte)(valsOut[z][dimX*(y)+(x)]) & 0xff));
					valMean2d=(double)(valsMean[z][dimX*(y)+(x)]);
					valVar2d=(double)(valsVar[z][dimX*(y)+(x)]) ;
					valVar3d=0;
					valMean3d=0;
					if(debug)System.out.println("Val="+val);
					if(debug)System.out.println("Valmean2d="+valMean2d);
					if(debug)System.out.println("Valvar2d="+valVar2d);
					for(int dz=-rayZup;dz<=rayZdown;dz++) {
						if((double)((int)((byte)(valsOut[z+dz][dimX*(y)+(x)]) & 0xff))>0.001) {
							valMean3d+=ratio*(double)(valsMean[z+dz][dimX*(y)+(x)]);
							valVar3d+=ratio*(double)(valsVar[z+dz][dimX*(y)+(x)]);
						}
					}
					if(debug)System.out.println("Valmean3d="+valMean3d);
					if(debug)System.out.println("Valvar3d="+valVar3d);
					if(val<0.00001)continue;
					if(valVar2d<1)nSig2d=0;
					else nSig2d=(val-valMean2d)/valVar2d;
					valInt=(int)Math.round(Math.max(Math.min( 255,valMean3d+valVar3d*nSig2d), 0));
					valsOut[z][dimX*(y)+(x)]=((byte)((   valInt  ) & 0xff));
					if(debug)System.out.println("nsig="+nSig2d);
					if(debug)System.out.println("valInt="+valInt);
					if(debug)System.out.println("valsOut="+((int)((byte)valsOut[z][dimX*(y)+(x)] & 0xff)));
				}			
			}
		}
		System.out.println("Effectivement le crop ImageByte sera : ");
		//		System.out.println("VitimageUtils.cropImageByte(imgOut,"+ rayX+", "+rayY+", "+rayZup+","+(dimX-2*rayX)+", "+(dimY-2*rayY)+", "+ (dimZ-rayZup-rayZdown));
		//		imgOut.show();
		//VitimageUtils.waitFor(10000);
//		imgOut=VitimageUtils.cropImageByte(imgOut, rayX, rayY, rayZup,dimX-2*rayX, dimY-2*rayY, dimZ-rayZup-rayZdown);
//		imgOut.resetDisplayRange();
		return imgOut;
	}
		
		
	

	public static ImagePlus normalizationSliceByte(ImagePlus img,int rayX,int rayY,int rayZup,int rayZdown,boolean statsExcludingBlackPoints) {
		double []voxS=VitimageUtils.getVoxelSizes(img);
		ImagePlus imgIn=new Duplicator().run(img);
		VitimageUtils.adjustImageCalibration(imgIn, img);
		int dimX=imgIn.getWidth();
		int dimY=imgIn.getHeight();
		int dimZ=imgIn.getStackSize();
		System.out.println("Effectivement dims avant="+dimX+","+dimY+","+dimZ);

		//Ajouter des donnees radiusZ en bas et en haut
		imgIn= uncropImageByte(imgIn,rayX,rayY,rayZup+rayZdown,dimX+2*rayX,dimY+2*rayY,dimZ+rayZup+rayZdown);
		imgIn.show();
		imgIn.setSlice(rayZup+1);
		IJ.run(imgIn, "Select All", "");
		IJ.run(imgIn, "Copy", "");
		for(int z=0;z<rayZup;z++) {
			imgIn.setSlice(z+1);
			IJ.run(imgIn, "Paste", "");
		}
		imgIn.setSlice(dimZ+rayZup);
		IJ.run(imgIn, "Select All", "");
		IJ.run(imgIn, "Copy", "");
		for(int z=0;z<rayZdown;z++) {
			imgIn.setSlice(dimZ+rayZup+rayZdown-rayZdown+z+1);
			IJ.run(imgIn, "Paste", "");
		}
		imgIn.hide();		
		dimX=imgIn.getWidth();
		dimY=imgIn.getHeight();
		dimZ=imgIn.getStackSize();
		System.out.println("Effectivement dims apres="+dimX+","+dimY+","+dimZ);
		ImagePlus imgOut=imgIn.duplicate();
		VitimageUtils.adjustImageCalibration(imgOut, imgIn);


		//Pour chaque voxel non nul, calculer les stats locales sur la slice et les stats locales en comptant les slices au dessus et dessous.
		byte[][] valsOut=new byte[dimZ][];
		double []stats2D;
		double []stats3D;
		double val;
		int valInt;
		double nSig2d;
		boolean debug;
		for(int z=0;z<dimZ;z++) {
			valsOut[z]=(byte[])imgOut.getStack().getProcessor(z+1).getPixels();
		}			
		for(int z=rayZup;z<dimZ-rayZdown;z++) {
			if(z%10==0)System.out.print("   "+z+"/"+dimZ);
			for(int x=rayX;x<dimX-rayX;x++) {
				for(int y=rayY;y<dimY-rayY;y++){
					val=(double)((int)((byte)(valsOut[z][dimX*(y)+(x)]) & 0xff));
					if(val<0.00001)continue;
					if(statsExcludingBlackPoints) {
						stats2D=VitimageUtils.statistics1DNoBlack(VitimageUtils.valuesOfBlock(imgIn, x-rayX, y-rayY, z, x+rayX, y+rayY, z));
						stats3D=VitimageUtils.statistics1DNoBlack(VitimageUtils.valuesOfBlock(imgIn, x-rayX, y-rayY, z-rayZup, x+rayX, y+rayY, z+rayZdown));
						nSig2d=(val-stats2D[0])/stats2D[1];
						valInt=(int)Math.round(Math.max(Math.min( 255,stats3D[0]+stats3D[1]*nSig2d), 0));
						valsOut[z][dimX*(y)+(x)]=((byte)((   valInt  ) & 0xff));
					}
					else {
						stats2D=VitimageUtils.statistics1D(VitimageUtils.valuesOfBlock(imgIn, x-rayX, y-rayY, z, x+rayX, y+rayY, z));
						stats3D=VitimageUtils.statistics1D(VitimageUtils.valuesOfBlock(imgIn, x-rayX, y-rayY, z-rayZup, x+rayX, y+rayY, z+rayZdown));
						nSig2d=(val-stats2D[0])/stats2D[1];
						valInt=(int)Math.round(Math.max(Math.min( 255,stats3D[0]+stats3D[1]*nSig2d), 0));
						valsOut[z][dimX*(y)+(x)]=((byte)((   valInt  ) & 0xff));
					}
				}			
			}
		}
		System.out.println("Effectivement le crop ImageByte sera : ");
		//		imgOut.show();
		//VitimageUtils.waitFor(10000);
		imgOut=VitimageUtils.cropImageByte(imgOut, rayX, rayY, rayZup,dimX-2*rayX, dimY-2*rayY, dimZ-rayZup-rayZdown);
		imgOut.resetDisplayRange();
		return imgOut;
	}

		
	
	
	
	
	
	
	
	
	
	
	
	

}
