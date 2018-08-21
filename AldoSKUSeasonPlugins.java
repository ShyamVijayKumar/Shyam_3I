package com.aldo.wc.season;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import wt.fc.WTObject;
import wt.part.WTPartMaster;
import wt.util.WTException;
import wt.util.WTPropertyVetoException;
import wt.vc.Mastered;

import com.lcs.wc.db.Criteria;
import com.lcs.wc.db.PreparedQueryStatement;
import com.lcs.wc.db.QueryColumn;
import com.lcs.wc.foundation.LCSQuery;
import com.lcs.wc.product.LCSProduct;
import com.lcs.wc.product.LCSSKU;

import com.lcs.wc.product.LCSSKUQuery;
import com.lcs.wc.season.LCSSeason;
import com.lcs.wc.season.LCSSeasonProductLink;
import com.lcs.wc.season.LCSSeasonQuery;
import com.lcs.wc.season.SeasonProductLocator;
import com.lcs.wc.util.FormatHelper;
import com.lcs.wc.util.LCSException;
import com.lcs.wc.util.LCSLog;
import com.lcs.wc.util.LCSProperties;
import com.lcs.wc.util.VersionHelper;

public class AldoSKUSeasonPlugins 
{

	public static final String CARRYOVER = "Carry Over";    
    public static final String MOVE = "Move";
    public static final String COPY_IN_PROGRESS = "COPY_IN_PROGRESS";
    final public static String flocked = LCSProperties.get("com.aldo.wc.product.AldoSKUPlugins.alFlocked");
			
    
	//Added to get SKU SEASON LINK START
	public static Collection<LCSSeasonProductLink> getSkuSeasonLinks(LCSSKU sku) throws WTException 
	{
		System.out.println("Inside getSkuSeasonLinks Method ");
		Collection<LCSSeasonProductLink> skuSeasonLinks = new ArrayList<LCSSeasonProductLink>();
		Collection<?> productSeasons = (new LCSSeasonQuery()).findSeasons(sku.getProduct());
		LCSLog.debug("Seasons  : " + productSeasons);
		if (productSeasons.isEmpty()) 
		{
			return null;
		}
		Iterator<?> seasonItr = productSeasons.iterator();
		// iterating seasons
		while (seasonItr.hasNext())
		{
			LCSSeason season = (LCSSeason) VersionHelper.latestIterationOf((Mastered) seasonItr.next());

			LCSSeasonProductLink skuSeasonLink = (LCSSeasonProductLink)
					LCSSeasonQuery.findSeasonProductLink(sku, season);
			if(skuSeasonLink!=null)
			{
				skuSeasonLinks.add(skuSeasonLink);
			}
		}
		System.out.println("skuSeasonLinks ------> "+skuSeasonLinks);
		return skuSeasonLinks;
	}
	//Added to get SKU SEASON LINK END


	//CR 004 NRF COLORWAY FUNCTION @SKUSEASON START

	public static void validateNRFColorForSKUSeason(WTObject object) throws WTException, WTPropertyVetoException 
	{
		System.out.println("END of plugin triggring triggring for  @SKUSEASONLEVEL------");
		if(object instanceof LCSSeasonProductLink)
		{
			LCSSeasonProductLink skuSeasonLink = (LCSSeasonProductLink) object;
			String skuSeasonLinkId = skuSeasonLink.toString();
			boolean seasonRemoved = skuSeasonLink.isSeasonRemoved();
			String seasonLinkType = skuSeasonLink.getSeasonLinkType();
			if(skuSeasonLinkId!=null && skuSeasonLinkId.indexOf(":")<0){
				return;
			}
			
			//CARRYOVER, MOVE AND COPY OF NRF CUSTOMIZATION START
			String carryOverMove = (String) wt.method.MethodContext.getContext().get("ADD_TO_SEASON_TYPE");
			if ("true".equals(wt.method.MethodContext.getContext().get(COPY_IN_PROGRESS)) || CARRYOVER.equals(carryOverMove) || MOVE.equals(carryOverMove)){
				return;
			}
			
			//CARRYOVER, MOVE AND COPY OF NRF CUSTOMIZATION END
			if (seasonLinkType!=null && FormatHelper.hasContent(seasonLinkType) && seasonLinkType.equalsIgnoreCase("SKU")) 
			{
				//LCSSeason season = (LCSSeason) VersionHelper.latestIterationOf(skuSeasonLink.getSeasonMaster());
				LCSSeason season = SeasonProductLocator.getSeasonRev(skuSeasonLink);
				System.out.println("season------:::: "+season.getIdentity());
				LCSSKU sku = (LCSSKU) VersionHelper.latestIterationOf(skuSeasonLink.getSkuMaster());
				System.out.println(" main sku::::::::: "+sku.getIdentity());
				WTPartMaster skuMaster = skuSeasonLink.getSkuMaster();
				String masterId = FormatHelper.getNumericObjectIdFromObject(skuSeasonLink.getSkuMaster());
				LCSSKU skuARev = LCSSKUQuery.getSKURevA(masterId);
				String mainMaterial = (String) skuARev.getValue("alMainMat");
				String detailedMaterial = (String) skuARev.getValue("alDetMaterial");
				LCSProduct product = SeasonProductLocator.getProductSeasonRev(sku);
				String nrfColorKey = (String) skuSeasonLink.getValue("alPrNRFColor");
				String flockedboolean =   (String) skuARev.getValue(flocked);
				if(!FormatHelper.hasContent(nrfColorKey)){
					return;
				}
				String colorwayNameConcat = mainMaterial + detailedMaterial + nrfColorKey+flockedboolean;
				System.out.println("FINAL VALUE CONCATED------- OF colorwayNameConcat "+colorwayNameConcat);
				Collection<WTPartMaster> allSkus =  LCSSeasonQuery.getSKUMastersForSeasonAndProduct(season, product,true);
				if(allSkus!=null && allSkus.size()>0)
				{	
					if(allSkus.contains(skuMaster))
					{
						System.out.println("Inside ----allSkus.contains(skuMaster) condition");
						allSkus.remove(skuMaster);
					}
					Iterator<WTPartMaster> allSkusItr = allSkus.iterator();					
					while (allSkusItr.hasNext()) 
					{
						WTPartMaster allSkuMaster = (WTPartMaster) allSkusItr.next();
						LCSSKU skuObj = LCSSKUQuery.getSKUVersion(allSkuMaster, "A");
						skuObj = (LCSSKU) VersionHelper.latestIterationOf(skuObj);
						System.out.println("iterated skuObj::::::::: "+skuObj.getIdentity());
						String allMainMaterial = (String) skuObj.getValue("alMainMat");
						//System.out.println("iterated allMainMaterial value is:::: "+allMainMaterial);
						String allDetailedMaterial = (String) skuObj.getValue("alDetMaterial");
						String flockedbooleanall = (String) skuObj.getValue(flocked);
						
						LCSSeasonProductLink allSkuSeasonLink = SeasonProductLocator.getSeasonProductLink(skuObj);
						//System.out.println("iterated allSkuSeasonLink value is   "+allSkuSeasonLink.getIdentity());
						if(allSkuSeasonLink!=null && allSkuSeasonLink.getSeasonLinkType().equalsIgnoreCase("SKU"))
						{
							String nrfColorAllKey = (String) allSkuSeasonLink.getValue("alPrNRFColor");
							System.out.println("iterated season name --  "+allSkuSeasonLink.getSeasonMaster().getName());
							//System.out.println("iterated nrfColorAllKey is of SKU SEASON method "+nrfColorAllKey);
							String allColorwayNameConcat = allMainMaterial + allDetailedMaterial + nrfColorAllKey+flockedbooleanall;
							System.out.println("iterated allColorwayNameConcat is of SKU SEASON method "+allColorwayNameConcat);
							if(colorwayNameConcat!=null && allColorwayNameConcat!=null && 
									FormatHelper.hasContent(colorwayNameConcat) && FormatHelper.hasContent(allColorwayNameConcat) && 
									colorwayNameConcat.equalsIgnoreCase(allColorwayNameConcat))
							{
								System.out.println("inside throw of if conditions ");
								throw new LCSException("Please select unique  NRF Color code for a Existing/New Colorway.");
							}
						}	
						
					}
				}				
			}
		}
		System.out.println("END of plugin triggring for  @SKUSEASONLEVEL------");
	}

	//CR 004 NRF COLORWAY FUNCTION @SKUSEASON END


	//CR 004 NRF COLORWAY FUNCTION @SKULEVEL START

	public static void validateNRFColorForSKU(WTObject object) throws WTException
	{
		
		
		System.out.println("START of plugin triggring for  @SKULEVEL------");
		if(object instanceof LCSSKU)
		{
			LCSSKU sku = (LCSSKU) object;
			//CARRYOVER, MOVE AND COPY OF NRF CUSTOMIZATION START
			String carryOverMove = (String) wt.method.MethodContext.getContext().get("ADD_TO_SEASON_TYPE");
			if ("true".equals(wt.method.MethodContext.getContext().get(COPY_IN_PROGRESS)) || CARRYOVER.equals(carryOverMove) || MOVE.equals(carryOverMove)){
				return;
			}
			
			//CARRYOVER, MOVE AND COPY OF NRF CUSTOMIZATION END
			WTPartMaster skuMaster = (WTPartMaster) sku.getMaster();
			String mainMaterial = (String) sku.getValue("alMainMat");
			//System.out.println("main material of sku "+mainMaterial);
			String detailedMaterial = (String) sku.getValue("alDetMaterial");
			
			String flockedboolean =   (String) sku.getValue(flocked);
			System.out.println("flockedboolean--------"+flockedboolean);
			//System.out.println("alDetMaterial material of sku "+detailedMaterial);
			String skuId = sku.toString();
			//System.out.println("skuId is::::: "+skuId);
			if(skuId!=null && FormatHelper.hasContent(skuId) && skuId.indexOf(":")>-1){
				PreparedQueryStatement statement = LCSSeasonQuery.findSeasonProductLinkQuery(FormatHelper.getNumericObjectIdFromObject(skuMaster), "");
				statement.appendAndIfNeeded();
				statement.appendCriteria(new Criteria(new QueryColumn("LCSSeasonProductLink", LCSSeasonProductLink.class, "seasonRemoved"), "1", Criteria.NOT_EQUAL_TO));
		        Collection links = LCSQuery.getObjectsFromResults(statement, "OR:com.lcs.wc.season.LCSSeasonProductLink:", "LCSSEASONPRODUCTLINK.IDA2A2");	
		       
		        if(links!=null && links.size()>0){
		        	Iterator<LCSSeasonProductLink> skuSeasonLinkItr = links.iterator();
		        	while (skuSeasonLinkItr.hasNext()) {
						LCSSeasonProductLink skuSeasonLink = (LCSSeasonProductLink) skuSeasonLinkItr.next();
						//System.out.println("skuSeasonLink is "+skuSeasonLink.getIdentity());
						//LCSSeasonProductLink skuSeasonLink = SeasonProductLocator.getSeasonProductLink(sku);
						if(skuSeasonLink!=null && skuSeasonLink.getSeasonLinkType().equalsIgnoreCase("SKU"))
						{
							LCSSeason season = SeasonProductLocator.getSeasonRev(skuSeasonLink);
							System.out.println("main sku season is :::"+season.getIdentity());
							String nrfColorKey = (String) skuSeasonLink.getValue("alPrNRFColor");
							//System.out.println(" nrfColorKey value is "+nrfColorKey);
							String colorwayNameConcat = mainMaterial + detailedMaterial + nrfColorKey + flockedboolean;
							System.out.println("main concated value is  "+colorwayNameConcat);
							//LCSProduct product = SeasonProductLocator.getProductSeasonRev(sku);
							LCSProduct product = SeasonProductLocator.getProductARev(sku);
							System.out.println("main product is "+product.getIdentity());
							LCSProduct productSeasonRev = SeasonProductLocator.getProductSeasonRev(skuSeasonLink);
							Collection<WTPartMaster> allSkus =  LCSSeasonQuery.getSKUMastersForSeasonAndProduct(season, productSeasonRev,true);
							System.out.println("allSkus.size() is "+allSkus.size());
							if(allSkus!=null && allSkus.size()>0)
								
							{						
								if(allSkus.contains(skuMaster))
								{
									System.out.println("inside remove skumaster");
									allSkus.remove(skuMaster);
								}
								Iterator<WTPartMaster> allSkusItr = allSkus.iterator();
								while (allSkusItr.hasNext()) 
								{
									WTPartMaster allSkuMaster = (WTPartMaster) allSkusItr.next();
									LCSSKU skuObj = LCSSKUQuery.getSKUVersion(allSkuMaster, "A");
									skuObj = (LCSSKU) VersionHelper.latestIterationOf(skuObj);
									System.out.println("iterated skuObj::::::::: "+skuObj.getIdentity());
									String allMainMaterial = (String) skuObj.getValue("alMainMat");
									//System.out.println("inside 2nd while loops of sku--- allMainMaterial "+allMainMaterial);
									String allDetailedMaterial = (String) skuObj.getValue("alDetMaterial");
									String allflocked =  (String) skuObj.getValue(flocked);
									System.out.println("allflocked----"+allflocked);
									//System.out.println("inside 2nd while loops of sku--- allDetailedMaterial "+allDetailedMaterial);

									//LCSSeasonProductLink allSkuSeasonLink = SeasonProductLocator.getSeasonProductLink(skuObj);
									PreparedQueryStatement allStatement = LCSSeasonQuery.findSeasonProductLinkQuery(FormatHelper.getNumericObjectIdFromObject(allSkuMaster), "");
									allStatement.appendAndIfNeeded();
									allStatement.appendCriteria(new Criteria(new QueryColumn("LCSSeasonProductLink", LCSSeasonProductLink.class, "seasonRemoved"), "1", Criteria.NOT_EQUAL_TO));
							        Collection allSKUlinks = LCSQuery.getObjectsFromResults(allStatement, "OR:com.lcs.wc.season.LCSSeasonProductLink:", "LCSSEASONPRODUCTLINK.IDA2A2");		
							        if(allSKUlinks!=null && allSKUlinks.size()>0)
							        {
							        	Iterator<LCSSeasonProductLink> allSkuSeasonLinkItr = allSKUlinks.iterator();
							        	//Iterator<LCSSeasonProductLink> allSkuSeasonLinkItr = links.iterator();
							        	while (allSkuSeasonLinkItr.hasNext())
							        	{
							        		LCSSeasonProductLink allSkuSeasonLink = (LCSSeasonProductLink) allSkuSeasonLinkItr.next();
							        		//LCSSeasonProductLink allSkuSeasonLink = (LCSSeasonProductLink) allSKUlinks.iterator().next();
								        	if(allSkuSeasonLink!=null && allSkuSeasonLink.getSeasonLinkType().equalsIgnoreCase("SKU"))
								        	{
												String nrfColorAllKey = (String) allSkuSeasonLink.getValue("alPrNRFColor");
												//System.out.println(" nrfColorAllKey value is of 3rd while loop "+nrfColorAllKey);
												System.out.println("iterated season name --  "+allSkuSeasonLink.getSeasonMaster().getName());													
												String allColorwayNameConcat = allMainMaterial + allDetailedMaterial + nrfColorAllKey + allflocked;
												System.out.println(" allColorwayNameConcat value is of 3rd while loop  "+allColorwayNameConcat);
												if(colorwayNameConcat!=null && allColorwayNameConcat!=null && 
														FormatHelper.hasContent(colorwayNameConcat) && FormatHelper.hasContent(allColorwayNameConcat) && 
														colorwayNameConcat.equalsIgnoreCase(allColorwayNameConcat))
												{
													throw new LCSException("Please select unique  Main Material/Detailed Material  for a Existing/New Colorway.");
												}
											}
							        	}
							        }
								}
							}
						}
					}					
		        }else{
		        	return;
		        }
		        		
			}
						
		}

		System.out.println("END of plugin triggring for  @SKULEVEL------");
		//CR 004 NRF COLORWAY FUNCTION @SKULEVEL END
	}
		
	
	
}





