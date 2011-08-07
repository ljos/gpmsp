package jef.video;

public class TileMap {

	public Get_tile_info tile_info;
	public int type;
	public int width;
	public int height;
	public int cols;
	public int rows;
	public int pen;

	public TileMap(Get_tile_info tile_get_info,int get_memory_offset, int type, int tile_width, int tile_height, int num_cols, int num_rows) {
		this.tile_info = tile_get_info;
		this.type = type;
		this.width = tile_width;
		this.height = tile_height;
		this.cols = num_cols;
		this.rows = num_rows;
	}

}