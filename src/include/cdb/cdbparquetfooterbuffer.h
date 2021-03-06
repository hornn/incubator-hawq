/* 
 *
 *
 *
 *
 */

#ifndef CDBPARQUETFOOTERBUFFER_H_
#define CDBPARQUETFOOTERBUFFER_H_

#include "postgres.h"
#include "storage/fd.h"

#define PARQUET_FOOTER_BUFFERMODE_READ 			0x1
#define PARQUET_FOOTER_BUFFERMODE_WRITE			0X2

#define PARQUET_FOOTER_BUFFER_CAPACITY_DEFAULT	16*1024

typedef struct ParquetFooterBuffer {
    
    int      Mode;            /* The mode of the buffer.                      */
    
	char    *FileName;		  /* The name of current file for error reporting.*/
	
    File 	 FileHandler;	  /* The handler of the file with offset adjusted.*/
    
	int		 FooterLength;	  /* The total length of the footer.			  */
                              /* FooterLength is useful for reading only.     */
    
	int		 FooterProcessed; /* The processed amount of bytes of this footer.*/
                              /* For reading, FooterProcessed saves bytes read*/
                              /* For writing, FooterProcessed saves the bytes */
                              /* already written to the buffer,unnecessary to */
                              /* be one legal footer structure data.          */
    
	int		 Capacity;        /* The capacity of this buffer.				  */
    
	int		 ValidLen;        /* The valid length of the content in buffer.   */
                              /* ValidLen is useful for reading only.         */

	int		 Cursor;	      /* The cursor for reading data in buffer.       */
                              /* For reading, Cursor is the first byte to read*/
                              /* For writing, Cursor is the last valid byte   */

	uint8_t *Head;	          /* The address of the buffer.                   */
    
    File     TmpFile;         /* The temporary file for footer write buffer.  */

} ParquetFooterBuffer;

#define PARQUET_FOOTER_BUFFER_MOVE_OK		0x0
#define PARQUET_FOOTER_BUFFER_MOVE_ERROR	0x1

ParquetFooterBuffer *createParquetFooterBuffer( File  dataFile, 
												char *filename,
												int   footerlength, 
												int   capacity,
												int   mode);

void freeParquetFooterBuffer( ParquetFooterBuffer *buffer);

int  prepareFooterBufferForwardReading( ParquetFooterBuffer  *buffer,
                                        int                   length,
                                        uint8_t             **ptr);

void writeToFooterBuffer( ParquetFooterBuffer *buffer,
                          uint8_t             *content,
                          int                  length);

void flushFooterBufferToTempFile( ParquetFooterBuffer *buffer);

int appendFooterBufferTempData(File targetFile, ParquetFooterBuffer *buffer);

#endif /* CDBPARQUETFOOTERBUFFER_H_  */
